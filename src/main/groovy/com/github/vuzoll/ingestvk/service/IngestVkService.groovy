package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.ingestvk.domain.VkCareerRecord
import com.github.vuzoll.ingestvk.domain.VkCity
import com.github.vuzoll.ingestvk.domain.VkCountry
import com.github.vuzoll.ingestvk.domain.VkMilitaryRecord
import com.github.vuzoll.ingestvk.domain.VkOccupation
import com.github.vuzoll.ingestvk.domain.VkPersonalBelief
import com.github.vuzoll.ingestvk.domain.VkProfile
import com.github.vuzoll.ingestvk.domain.VkRelationPartner
import com.github.vuzoll.ingestvk.domain.VkRelative
import com.github.vuzoll.ingestvk.domain.VkSchoolRecord
import com.github.vuzoll.ingestvk.domain.VkUniversityRecord
import com.github.vuzoll.ingestvk.repository.VkProfileRepository
import com.github.vuzoll.tasks.service.DurableJob
import com.vk.api.sdk.objects.base.BaseObject
import com.vk.api.sdk.objects.base.Country
import com.vk.api.sdk.objects.users.Career
import com.vk.api.sdk.objects.users.Military
import com.vk.api.sdk.objects.users.Occupation
import com.vk.api.sdk.objects.users.Personal
import com.vk.api.sdk.objects.users.Relative
import com.vk.api.sdk.objects.users.School
import com.vk.api.sdk.objects.users.University
import com.vk.api.sdk.objects.users.UserFull
import com.vk.api.sdk.objects.users.UserMin
import org.apache.commons.lang3.RandomUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class IngestVkService {

    static final Integer REQUEST_SIZE = Integer.parseInt(System.getenv('VK_API_REQUEST_SIZE') ?: '100')

    @Autowired
    VkProfileRepository vkProfileRepository

    @Autowired
    VkApiService vkApiService

    DurableJob ingestUsingRandomizedBfsJob(String datasetName, Integer seedId) {
        new BasicIngestJob("ingest vk data into dataset=${datasetName} using randomized bfs", datasetName) {

            @Override
            Collection<Integer> getSeedIds(Closure statusUpdater) {
                [ seedId ]
            }

            @Override
            VkProfile getNextProfileToIngest(Closure statusUpdater) {
                int randomVkProfileIndex = RandomUtils.nextInt(0, vkProfileRepository.countByDatasetName(datasetName))
                return vkProfileRepository.findByDatasetName(datasetName, new PageRequest(randomVkProfileIndex, 1)).content.first()
            }

            @Override
            boolean finished(Closure statusUpdater) {
                false
            }

            @Override
            boolean acceptProfile(Closure statusUpdater, VkProfile vkProfile) {
                true
            }

            @Override
            VkProfile processProfile(Closure statusUpdater, VkProfile vkProfile) {
                vkProfile
            }
        }
    }

    DurableJob ingestUsingBfsJob(String datasetName, Integer seedId) {
        new BasicIngestJob("ingest vk data into dataset=${datasetName} using bfs", datasetName) {

            int indexOfRecordToIngestNext
            int ingestionIndexForNextRecord

            @Override
            void initSelf(Closure statusUpdater) {
                super.initSelf(statusUpdater)
                indexOfRecordToIngestNext = 0
                ingestionIndexForNextRecord = vkProfileRepository.countByDatasetName(datasetName)
            }

            @Override
            Collection<Integer> getSeedIds(Closure statusUpdater) {
                [ seedId ]
            }

            @Override
            boolean finished(Closure statusUpdater) {
                indexOfRecordToIngestNext >= vkProfileRepository.countByDatasetName(datasetName)
            }

            @Override
            VkProfile getNextProfileToIngest(Closure statusUpdater) {
                VkProfile nextProfileToIngest = vkProfileRepository.findOneByDatasetNameAndIngestionIndex(datasetName, indexOfRecordToIngestNext)
                indexOfRecordToIngestNext++
                return nextProfileToIngest
            }

            @Override
            boolean acceptProfile(Closure statusUpdater, VkProfile vkProfile) {
                true
            }

            @Override
            VkProfile processProfile(Closure statusUpdater, VkProfile vkProfile) {
                vkProfile.ingestionIndex = ingestionIndexForNextRecord
                ingestionIndexForNextRecord++
                return vkProfile
            }
        }
    }

    DurableJob ingestUsingGroupBfsJob(String datasetName, Collection<String> seedGroupIds, Collection<Integer> universityIdsToAccept) {
        new BasicIngestJob("ingest vk data into dataset=${datasetName} using group bfs", datasetName) {

            int indexOfRecordToIngestNext
            int ingestionIndexForNextRecord

            @Override
            void initSelf(Closure statusUpdater) {
                super.initSelf(statusUpdater)
                indexOfRecordToIngestNext = 0
                ingestionIndexForNextRecord = vkProfileRepository.countByDatasetName(datasetName)
            }

            @Override
            Collection<Integer> getSeedIds(Closure statusUpdater) {
                seedGroupIds.collectMany { String groupId ->
                    Collection<Integer> groupMembersIds = vkApiService.getGroupMembersIds(groupId)
                    statusUpdater publish: 'always', message: "use group with id=${groupId} and ${groupMembersIds.size()} profiles as seed group..."
                    return groupMembersIds
                }
            }

            @Override
            boolean finished(Closure statusUpdater) {
                indexOfRecordToIngestNext >= vkProfileRepository.countByDatasetName(datasetName)
            }

            @Override
            VkProfile getNextProfileToIngest(Closure statusUpdater) {
                VkProfile nextProfileToIngest = vkProfileRepository.findOneByDatasetNameAndIngestionIndex(datasetName, indexOfRecordToIngestNext)
                indexOfRecordToIngestNext++
                return nextProfileToIngest
            }

            @Override
            boolean acceptProfile(Closure statusUpdater, VkProfile vkProfile) {
                if (universityIdsToAccept.empty) {
                    return true
                } else {
                    return (((vkProfile.universityRecords?.universityId?:[]).find({ universityIdsToAccept.contains(it) })) != null)
                }
            }

            @Override
            VkProfile processProfile(Closure statusUpdater, VkProfile vkProfile) {
                vkProfile.ingestionIndex = ingestionIndexForNextRecord
                ingestionIndexForNextRecord++
                return vkProfile
            }
        }
    }


    abstract class BasicIngestJob extends DurableJob {

        final String datasetName

        int ingestedCount
        boolean seedsIngested

        BasicIngestJob(String jobName, String datasetName) {
            super(jobName)
            this.datasetName = datasetName
        }


        abstract Collection<Integer> getSeedIds(Closure statusUpdater)

        abstract VkProfile getNextProfileToIngest(Closure statusUpdater)

        abstract boolean finished(Closure statusUpdater)

        abstract boolean acceptProfile(Closure statusUpdater, VkProfile vkProfile)

        abstract VkProfile processProfile(Closure statusUpdater, VkProfile vkProfile)


        @Override
        void initSelf(Closure statusUpdater) {
            ingestedCount = 0
            seedsIngested = false
        }

        @Override
        void doSomething(Closure statusUpdater) {
            List<Integer> idsToIngest = idsToIngest(statusUpdater)

            while (!idsToIngest.empty) {
                int lastIndex = Math.min(idsToIngest.size(), REQUEST_SIZE)

                statusUpdater publish: 'never', message: "ingesting ${lastIndex} new profiles (${idsToIngest.size()} in the queue)..."
                Collection<VkProfile> profilesToSave = this.profilesToSave(statusUpdater, idsToIngest.subList(0, lastIndex))

                statusUpdater publish: 'never', message: "saving ${profilesToSave.size()} new profiles to database..."
                vkProfileRepository.save(profilesToSave)
                ingestedCount += profilesToSave.size()

                idsToIngest = idsToIngest.subList(lastIndex, idsToIngest.size())
            }

            statusUpdater publish: 'always', message: "already ingested ${ingestedCount} profiles, current dataset size is ${vkProfileRepository.countByDatasetName(datasetName)}"

            if (finished(statusUpdater)) {
                statusUpdater publish: 'always', message: 'ingestion finished successfully'
                markFinished()
            }
        }

        List<Integer> idsToIngest(Closure statusUpdater) {
            List<Integer> idsToIngest = new ArrayList<>()
            if (!seedsIngested) {
                statusUpdater publish: 'optional', message: 'ingestion just started - first will check seed profiles...'
                Collection<Integer> seedIds = getSeedIds(statusUpdater)
                statusUpdater publish: 'always', message: "ingestion just started - generated ${seedIds.size()} seed profiles..."

                seedIds = seedIds.unique().findAll({ vkProfileRepository.findOneByDatasetNameAndVkId(datasetName, it) == null })
                statusUpdater publish: 'always', message: "ingestion just started - ${seedIds.size()} unique profiles are not ingested yet..."

                idsToIngest.addAll(seedIds)
                seedsIngested = true
            } else {
                VkProfile nextVkProfileToIngest = getNextProfileToIngest(statusUpdater)
                statusUpdater publish: 'optional', message: "using profile with id=${nextVkProfileToIngest.vkId} for the next ingestion iteration..."
                statusUpdater publish: 'always', message: "profile with id=${nextVkProfileToIngest.vkId} has ${nextVkProfileToIngest.friendsIds.size()} friends, finding new profiles..."
                Collection<Integer> newProfileIds = nextVkProfileToIngest.friendsIds.findAll({ Integer friendVkId ->
                    vkProfileRepository.findOneByDatasetNameAndVkId(datasetName, friendVkId) == null
                })
                statusUpdater publish: 'always', message: "using profile with id=$nextVkProfileToIngest.vkId ${newProfileIds.size()} new profiles found, ingesting them..."

                idsToIngest.addAll(newProfileIds)
            }
            return idsToIngest
        }

        Collection<VkProfile> profilesToSave(Closure statusUpdater, Collection<Integer> idsToSave) {
            vkApiService
                    .ingestVkProfilesById(idsToSave)
                    .collect(this.&toVkProfile)
                    .findAll(this.&acceptProfile.curry(statusUpdater))
                    .collect(this.&loadAdditionalInformation)
                    .collect(this.&processProfile.curry(statusUpdater))
        }

        VkProfile toVkProfile(UserFull vkApiUser) {
            VkProfile vkProfile = new VkProfile()
            vkProfile.vkId = vkApiUser.id
            vkProfile.vkDomain = vkApiUser.domain
            vkProfile.vkLastSeen = vkApiUser.lastSeen?.time
            vkProfile.vkActive = (vkApiUser.deactivated == null)

            vkProfile.firstName = vkApiUser.firstName
            vkProfile.lastName = vkApiUser.lastName
            vkProfile.maidenName = vkApiUser.maidenName
            vkProfile.middleName = vkApiUser.nickname
            vkProfile.mobilePhone = vkApiUser.mobilePhone
            vkProfile.homePhone = vkApiUser.homePhone
            vkProfile.relationPartner = toVkRelationPartner(vkApiUser.relationPartner)
            vkProfile.screenName = vkApiUser.screenName
            vkProfile.site = vkApiUser.site

            vkProfile.ingestedTimestamp = System.currentTimeMillis()
            vkProfile.datasetName = datasetName

            vkProfile.birthday = vkApiUser.bdate
            vkProfile.city = toVkCity(vkApiUser.city)
            vkProfile.country = toVkCountry(vkApiUser.country)
            vkProfile.homeTown = vkApiUser.homeTown
            vkProfile.sex = vkApiUser.sex

            vkProfile.occupation = toVkOccupation(vkApiUser.occupation)
            vkProfile.careerRecords = vkApiUser.career?.collect(this.&toVkCareerRecord)?.findAll({ it != null }) ?: []
            vkProfile.universityRecords = collectUniversityRecords(vkApiUser)
            vkProfile.militaryRecords = vkApiUser.military?.collect(this.&toVkMilitaryRecord)?.findAll({ it != null }) ?: []
            vkProfile.schoolRecords = vkApiUser.schools?.collect(this.&toVkSchoolRecord)?.findAll({ it != null }) ?: []

            vkProfile.skypeLogin = vkApiUser.skype
            vkProfile.facebookId = vkApiUser.facebook
            vkProfile.facebookName = vkApiUser.facebookName
            vkProfile.twitterId = vkApiUser.twitter
            vkProfile.livejournalId = vkApiUser.livejournal
            vkProfile.instagramId = vkApiUser.instagram
            vkProfile.verified = vkApiUser.verified

            vkProfile.about = vkApiUser.about
            vkProfile.activities = vkApiUser.activities
            vkProfile.books = vkApiUser.books
            vkProfile.games = vkApiUser.games
            vkProfile.interests = vkApiUser.interests
            vkProfile.movies = vkApiUser.movies
            vkProfile.music = vkApiUser.music
            vkProfile.personalBelief = toVkPersonalBelief(vkApiUser.personal)
            vkProfile.quotes = vkApiUser.quotes
            vkProfile.relatives = vkApiUser.relatives?.collect(this.&toVkRelative)?.findAll({ it != null }) ?: []
            vkProfile.relationStatus = vkApiUser.relation
            vkProfile.tvShows = vkApiUser.tv

            return vkProfile
        }

        VkProfile loadAdditionalInformation(VkProfile vkProfile) {
            vkProfile.friendsIds = vkApiService.getFriendsIds(vkProfile.vkId)
            return vkProfile
        }

        Set<VkUniversityRecord> collectUniversityRecords(UserFull vkApiUser) {
            Set<VkUniversityRecord> universityRecords = vkApiUser.universities?.collect(this.&toVkUniversityRecord) ?: []
            if (universityRecords.isEmpty()) {
                universityRecords.add(toVkUniversityRecordFromUser(vkApiUser))
            }

            return universityRecords.findAll({ it != null })
        }

        VkCareerRecord toVkCareerRecord(Career vkApiCareer) {
            if (vkApiCareer == null) {
                return null
            }

            VkCareerRecord vkCareerRecord = new VkCareerRecord()
            vkCareerRecord.groupId = vkApiCareer.groupId
            vkCareerRecord.company = vkApiCareer.company
            vkCareerRecord.countryId = vkApiCareer.countryId
            vkCareerRecord.cityId = vkApiCareer.cityId
            vkCareerRecord.from = vkApiCareer.from
            vkCareerRecord.until = vkApiCareer.until
            vkCareerRecord.position = vkApiCareer.position

            return vkCareerRecord
        }

        VkCity toVkCity(BaseObject vkApiCity) {
            if (vkApiCity == null) {
                return null
            }

            VkCity vkCity = new VkCity()
            vkCity.vkId = vkApiCity.id
            vkCity.name = vkApiCity.title

            return vkCity
        }

        VkCountry toVkCountry(Country vkApiCountry) {
            if (vkApiCountry == null) {
                return null
            }

            VkCountry vkCountry = new VkCountry()
            vkCountry.vkId = vkApiCountry.id
            vkCountry.name = vkApiCountry.title

            return vkCountry
        }

        VkUniversityRecord toVkUniversityRecord(University vkApiUniversity) {
            if (vkApiUniversity == null) {
                return null
            }

            VkUniversityRecord vkUniversityRecord = new VkUniversityRecord()
            vkUniversityRecord.universityId = vkApiUniversity.id
            vkUniversityRecord.countryId = vkApiUniversity.country
            vkUniversityRecord.cityId = vkApiUniversity.city
            vkUniversityRecord.universityName = vkApiUniversity.name
            vkUniversityRecord.facultyId = vkApiUniversity.faculty
            vkUniversityRecord.facultyName = vkApiUniversity.facultyName
            vkUniversityRecord.chairId = vkApiUniversity.chair
            vkUniversityRecord.chairName = vkApiUniversity.chairName
            vkUniversityRecord.graduationYear = vkApiUniversity.graduation
            vkUniversityRecord.educationForm = vkApiUniversity.educationForm
            vkUniversityRecord.educationStatus = vkApiUniversity.educationStatus

            if (vkUniversityRecord.universityId == null || vkUniversityRecord.universityId == 0) {
                return null
            }

            return vkUniversityRecord
        }

        VkUniversityRecord toVkUniversityRecordFromUser(UserFull vkApiUser) {
            if (vkApiUser == null) {
                return null
            }

            VkUniversityRecord vkUniversityRecord = new VkUniversityRecord()
            vkUniversityRecord.universityId = vkApiUser.university
            vkUniversityRecord.universityName = vkApiUser.universityName
            vkUniversityRecord.facultyId = vkApiUser.faculty
            vkUniversityRecord.facultyName = vkApiUser.facultyName
            vkUniversityRecord.graduationYear = vkApiUser.graduation
            vkUniversityRecord.educationForm = vkApiUser.educationForm
            vkUniversityRecord.educationStatus = vkApiUser.educationStatus

            if (vkUniversityRecord.universityId == null || vkUniversityRecord.universityId == 0) {
                return null
            }

            return vkUniversityRecord
        }

        VkMilitaryRecord toVkMilitaryRecord(Military vkApiMilitary) {
            if (vkApiMilitary == null) {
                return null
            }

            VkMilitaryRecord vkMilitaryRecord = new VkMilitaryRecord()
            vkMilitaryRecord.vkId = vkApiMilitary.unitId
            vkMilitaryRecord.unit = vkApiMilitary.unit
            vkMilitaryRecord.countryId = vkApiMilitary.countryId
            vkMilitaryRecord.from = vkApiMilitary.from
            vkMilitaryRecord.until = vkApiMilitary.until

            return vkMilitaryRecord
        }

        VkOccupation toVkOccupation(Occupation vkApiOccupation) {
            if (vkApiOccupation == null) {
                return null
            }

            VkOccupation vkOccupation = new VkOccupation()
            vkOccupation.vkId = vkApiOccupation.id
            vkOccupation.type = vkApiOccupation.type
            vkOccupation.name = vkApiOccupation.name

            return vkOccupation
        }

        VkPersonalBelief toVkPersonalBelief(Personal vkApiPersonal) {
            if (vkApiPersonal == null) {
                return null
            }

            VkPersonalBelief vkPersonalBelief = new VkPersonalBelief()
            vkPersonalBelief.politicalBelief = vkApiPersonal?.political
            vkPersonalBelief.languages = vkApiPersonal?.langs ?: []
            vkPersonalBelief.religionBelief = vkApiPersonal?.religion
            vkPersonalBelief.inspiredBy = vkApiPersonal?.inspiredBy
            vkPersonalBelief.importantInPeople = vkApiPersonal?.peopleMain
            vkPersonalBelief.importantInLife = vkApiPersonal?.lifeMain
            vkPersonalBelief.smokingAttitude = vkApiPersonal?.smoking
            vkPersonalBelief.alcoholAttitude = vkApiPersonal?.alcohol

            return vkPersonalBelief
        }

        VkRelative toVkRelative(Relative vkApiRelative) {
            if (vkApiRelative == null) {
                return null
            }

            VkRelative vkRelative = new VkRelative()
            vkRelative.vkId = vkApiRelative.id
            vkRelative.type = vkApiRelative.type

            return vkRelative
        }

        VkSchoolRecord toVkSchoolRecord(School vkApiSchool) {
            if (vkApiSchool == null) {
                return null
            }

            VkSchoolRecord vkSchoolRecord = new VkSchoolRecord()
            vkSchoolRecord.vkId = vkApiSchool.id
            vkSchoolRecord.countryId = vkApiSchool.country
            vkSchoolRecord.cityId = vkApiSchool.city
            vkSchoolRecord.name = vkApiSchool.name
            vkSchoolRecord.yearFrom = vkApiSchool.yearFrom
            vkSchoolRecord.yearTo = vkApiSchool.yearTo
            vkSchoolRecord.graduationYear = vkApiSchool.yearGraduated
            vkSchoolRecord.classLetter = vkApiSchool.className
            vkSchoolRecord.typeId = vkApiSchool.type
            vkSchoolRecord.typeName = vkApiSchool.typeStr

            return vkSchoolRecord
        }

        VkRelationPartner toVkRelationPartner(UserMin vkApiRelationPartner) {
            if (vkApiRelationPartner == null) {
                return null
            }

            VkRelationPartner vkRelationPartner = new VkRelationPartner()
            vkRelationPartner.vkId = vkApiRelationPartner.id
            vkRelationPartner.firstName = vkApiRelationPartner.firstName
            vkRelationPartner.lastName = vkApiRelationPartner.lastName

            return vkRelationPartner
        }
    }
}
