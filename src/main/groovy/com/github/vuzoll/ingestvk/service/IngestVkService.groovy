package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.ingestvk.domain.job.IngestJob
import com.github.vuzoll.ingestvk.domain.job.IngestJobLog
import com.github.vuzoll.ingestvk.domain.job.JobStatus
import com.github.vuzoll.ingestvk.domain.vk.VkCareerRecord
import com.github.vuzoll.ingestvk.domain.vk.VkCity
import com.github.vuzoll.ingestvk.domain.vk.VkCountry
import com.github.vuzoll.ingestvk.domain.vk.VkMilitaryRecord
import com.github.vuzoll.ingestvk.domain.vk.VkOccupation
import com.github.vuzoll.ingestvk.domain.vk.VkPersonalBelief
import com.github.vuzoll.ingestvk.domain.vk.VkProfile
import com.github.vuzoll.ingestvk.domain.vk.VkRelationPartner
import com.github.vuzoll.ingestvk.domain.vk.VkRelative
import com.github.vuzoll.ingestvk.domain.vk.VkSchoolRecord
import com.github.vuzoll.ingestvk.domain.vk.VkUniversityRecord
import com.github.vuzoll.ingestvk.repository.job.IngestJobRepository
import com.github.vuzoll.ingestvk.repository.vk.VkProfileRepository
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
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomUtils
import org.joda.time.Period
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
@Slf4j
class IngestVkService {

    static final long LOG_DELTA = TimeUnit.HOURS.toMillis(1)

    static final String DEFAULT_SEED_ID = System.getenv('INGEST_VK_DEFAULT_SEED_ID') ?: '3542756'

    static final Integer REQUEST_SIZE = Integer.parseInt(System.getenv('INGEST_VK_REQUEST_SIZE') ?: '100')

    static final PeriodFormatter TIME_LIMIT_FORMAT = new PeriodFormatterBuilder()
            .appendHours().appendSuffix('h')
            .appendMinutes().appendSuffix('min')
            .appendSeconds().appendSuffix('sec')
            .toFormatter()

    @Autowired
    VkProfileRepository vkProfileRepository

    @Autowired
    IngestJobRepository ingestJobRepository

    @Autowired
    VkApiService vkApiService

    void bfsIngest(IngestJob jobToStart) {
        int indexToIngest = 0

        ingest(jobToStart, { IngestJob ingestJob ->
            log.info "JobId=${ingestJob.id}: choosing profile for next ingestion iteration: ${indexToIngest} / ${ingestJob.datasetSize}"
            VkProfile nextProfileToIngest = vkProfileRepository.findOneByIngestionIndex(indexToIngest)
            indexToIngest++
            return nextProfileToIngest
        })
    }

    void randomizedBfsIngest(IngestJob jobToStart) {
        ingest(jobToStart, { IngestJob ingestJob ->
            int randomVkProfileIndex = RandomUtils.nextInt(0, ingestJob.datasetSize)
            log.info "JobId=${ingestJob.id}: choosing random profile for next ingestion iteration: ${randomVkProfileIndex} / ${ingestJob.datasetSize}"
            return vkProfileRepository.findAll(new PageRequest(randomVkProfileIndex, 1)).content.first()
        })
    }

    private void ingest(IngestJob ingestJob, Closure<VkProfile> getNextProfileToIngest) {
        try {
            ingestJob.startTimestamp = System.currentTimeMillis()
            ingestJob.startTime = LocalDateTime.now().toString()
            ingestJob.ingestedCount = 0
            ingestJob.datasetSize = vkProfileRepository.count()
            ingestJob.lastUpdateTime = ingestJob.startTime
            ingestJob.timeTaken = '0sec'
            ingestJob = ingestJobRepository.save ingestJob

            int ingestionIndex = ingestJob.datasetSize

            while (true) {
                log.info "JobId=${ingestJob.id}: ingestion already has taken ${toDurationString(System.currentTimeMillis() - ingestJob.startTimestamp)}"
                log.info "JobId=${ingestJob.id}: current dataset size is ${ingestJob.datasetSize} records"
                log.info "JobId=${ingestJob.id}: already ingested ${ingestJob.ingestedCount} records"

                if (ingestJob.request.timeLimit != null && System.currentTimeMillis() >= ingestJob.startTimestamp + fromDurationString(ingestJob.request.timeLimit)) {
                    ingestJob.message = 'time limit is reached'
                    log.info "JobId=${ingestJob.id}: ${ingestJob.message}"
                    break
                }

                if (ingestJob.request.dataSizeLimit != null && ingestJob.datasetSize >= ingestJob.request.dataSizeLimit) {
                    ingestJob.message = 'dataset size limit is reached'
                    log.info "JobId=${ingestJob.id}: ${ingestJob.message}"
                    break
                }

                if (ingestJob.request.ingestedLimit != null && ingestJob.ingestedCount >= ingestJob.request.ingestedLimit) {
                    ingestJob.message = 'ingested records limit is reached'
                    log.info "JobId=${ingestJob.id}: ${ingestJob.message}"
                    break
                }

                if (ingestJob.status == JobStatus.STOPPING.toString()) {
                    ingestJob.message = 'stopped by client request'
                    log.info "JobId=${ingestJob.id}: ${ingestJob.message}"
                    break
                }

                if (ingestJob.datasetSize == 0) {
                    Integer seedId = Integer.parseInt(ingestJob.request.parameters?.getOrDefault('seedId', DEFAULT_SEED_ID) ?: DEFAULT_SEED_ID)
                    log.warn "JobId=${ingestJob.id}: dataset is empty - using seed profile with id=$seedId to initialize it..."

                    VkProfile seedProfile = toVkProfile(vkApiService.ingestVkProfileById(seedId))
                    seedProfile.ingestionIndex = ingestionIndex
                    vkProfileRepository.save seedProfile
                    ingestionIndex++
                    ingestJob.ingestedCount++
                    ingestJob.datasetSize = vkProfileRepository.count()
                    continue
                }

                VkProfile nextVkProfileToIngest = getNextProfileToIngest.call(ingestJob)

                log.info "JobId=${ingestJob.id}: using profile with id=$nextVkProfileToIngest.vkId for the next ingestion iteration..."
                log.info "JobId=${ingestJob.id}: profile with id=$nextVkProfileToIngest.vkId has ${nextVkProfileToIngest.friendsIds.size()} friends, finding new profiles..."
                Collection<Integer> newProfileIds = nextVkProfileToIngest.friendsIds.findAll({ Integer friendVkId ->
                    vkProfileRepository.findOneByVkId(friendVkId) == null
                })
                log.info "JobId=${ingestJob.id}: using profile with id=$nextVkProfileToIngest.vkId ${newProfileIds.size()} new profiles found, ingesting them..."

                List<Integer> idsToIngest = new ArrayList<>()
                idsToIngest.addAll(newProfileIds)
                while (!idsToIngest.empty) {
                    int lastIndex = Math.min(idsToIngest.size(), REQUEST_SIZE)

                    log.info "JobId=${ingestJob.id}: ingesting ${lastIndex} new profiles (${idsToIngest.size()} in the queue)..."
                    Collection<UserFull> newProfiles = vkApiService.ingestVkProfilesById(idsToIngest.subList(0, lastIndex))

                    log.info "JobId=${ingestJob.id}: saving ${newProfiles.size()} new profiles to database..."
                    vkProfileRepository.save( newProfiles.collect(this.&toVkProfile).collect({ it.ingestionIndex = ingestionIndex; ingestionIndex++; return it }) )
                    ingestJob.ingestedCount += newProfiles.size()

                    idsToIngest = idsToIngest.subList(lastIndex, idsToIngest.size())

                    log.info "JobId=${ingestJob.id}: updating job status..."
                    int ingestedCount = ingestJob.ingestedCount
                    ingestJob = ingestJobRepository.findOne(ingestJob.id)

                    ingestJob.ingestedCount = ingestedCount
                    ingestJob.datasetSize = vkProfileRepository.count()
                    ingestJob.lastUpdateTime = LocalDateTime.now().toString()
                    ingestJob.timeTaken = toDurationString(System.currentTimeMillis() - ingestJob.startTimestamp)

                    if (ingestJob.ingestJobLogs == null || ingestJob.ingestJobLogs.empty || System.currentTimeMillis() - ingestJob.ingestJobLogs.timestamp.max() > LOG_DELTA) {
                        log.info 'Saving IngestJob log record...'
                        IngestJobLog ingestJobLog = new IngestJobLog()
                        ingestJobLog.timestamp = System.currentTimeMillis()
                        ingestJobLog.time = LocalDateTime.now().toString()
                        ingestJobLog.timeTaken = ingestJob.timeTaken
                        ingestJobLog.status = ingestJob.status
                        ingestJobLog.datasetSize = ingestJob.datasetSize
                        ingestJobLog.ingestedCount = ingestJob.ingestedCount
                        ingestJob.ingestJobLogs = ingestJob.ingestJobLogs == null ? [ ingestJobLog ] : ingestJob.ingestJobLogs + ingestJobLog
                    }

                    ingestJobRepository.save(ingestJob)
                }
            }

            ingestJob.endTime = LocalDateTime.now().toString()
            ingestJob.lastUpdateTime = ingestJob.endTime
            ingestJob.timeTaken = toDurationString(System.currentTimeMillis() - ingestJob.startTimestamp)
            ingestJob.status = JobStatus.COMPLETED.toString()
            ingestJobRepository.save ingestJob
        } catch (e) {
            log.error("JobId=${ingestJob.id}: ingestion failed", e)

            ingestJob.message = "Failed because of ${e.class.name}, with message: ${e.message}"
            ingestJob.datasetSize = vkProfileRepository.count() as Integer
            ingestJob.endTime = LocalDateTime.now().toString()
            ingestJob.lastUpdateTime = ingestJob.endTime
            ingestJob.timeTaken = toDurationString(System.currentTimeMillis() - ingestJob.startTimestamp)
            ingestJob.status = JobStatus.FAILED.toString()
            ingestJobRepository.save ingestJob

            throw e
        }
    }

    private VkProfile toVkProfile(UserFull vkApiUser) {
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

        vkProfile.friendsIds = vkApiService.getFriendsIds(vkApiUser.id)

        vkProfile.ingestedTimestamp = System.currentTimeMillis()

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

    Set<VkUniversityRecord> collectUniversityRecords(UserFull vkApiUser) {
        Set<VkUniversityRecord> universityRecords = vkApiUser.universities?.collect(this.&toVkUniversityRecord) ?: []
        if (universityRecords.isEmpty()) {
            universityRecords.add(toVkUniversityRecord(vkApiUser))
        }

        return universityRecords.findAll({ it != null })
    }

    private VkCareerRecord toVkCareerRecord(Career vkApiCareer) {
        if (vkApiCareer == null) {
            return null
        }

        VkCareerRecord vkCareerRecord = new VkCareerRecord()
        vkCareerRecord.groupId = vkApiCareer.groupId
        vkCareerRecord.countryId = vkApiCareer.countryId
        vkCareerRecord.cityId = vkApiCareer.cityId
        vkCareerRecord.from = vkApiCareer.from
        vkCareerRecord.until = vkApiCareer.until
        vkCareerRecord.position = vkApiCareer.position

        return vkCareerRecord
    }

    private VkCity toVkCity(BaseObject vkApiCity) {
        if (vkApiCity == null) {
            return null
        }

        VkCity vkCity = new VkCity()
        vkCity.vkId = vkApiCity.id
        vkCity.name = vkApiCity.title

        return vkCity
    }

    private VkCountry toVkCountry(Country vkApiCountry) {
        if (vkApiCountry == null) {
            return null
        }

        VkCountry vkCountry = new VkCountry()
        vkCountry.vkId = vkApiCountry.id
        vkCountry.name = vkApiCountry.title

        return vkCountry
    }

    private VkUniversityRecord toVkUniversityRecord(University vkApiUniversity) {
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

    private VkUniversityRecord toVkUniversityRecord(UserFull vkApiUser) {
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

    private VkMilitaryRecord toVkMilitaryRecord(Military vkApiMilitary) {
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

    private VkOccupation toVkOccupation(Occupation vkApiOccupation) {
        if (vkApiOccupation == null) {
            return null
        }

        VkOccupation vkOccupation = new VkOccupation()
        vkOccupation.vkId = vkApiOccupation.id
        vkOccupation.type = vkApiOccupation.type
        vkOccupation.name = vkApiOccupation.name

        return vkOccupation
    }

    private VkPersonalBelief toVkPersonalBelief(Personal vkApiPersonal) {
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

    private VkRelative toVkRelative(Relative vkApiRelative) {
        if (vkApiRelative == null) {
            return null
        }

        VkRelative vkRelative = new VkRelative()
        vkRelative.vkId = vkApiRelative.id
        vkRelative.type = vkApiRelative.type

        return vkRelative
    }

    private VkSchoolRecord toVkSchoolRecord(School vkApiSchool) {
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

    private VkRelationPartner toVkRelationPartner(UserMin vkApiRelationPartner) {
        if (vkApiRelationPartner == null) {
            return null
        }

        VkRelationPartner vkRelationPartner = new VkRelationPartner()
        vkRelationPartner.vkId = vkApiRelationPartner.id
        vkRelationPartner.firstName = vkApiRelationPartner.firstName
        vkRelationPartner.lastName = vkApiRelationPartner.lastName

        return vkRelationPartner
    }

    static long fromDurationString(String durationAsString) {
        TIME_LIMIT_FORMAT.parsePeriod(durationAsString).toStandardDuration().getStandardSeconds() * 1000
    }

    static String toDurationString(long duration) {
        String durationString = TIME_LIMIT_FORMAT.print(new Period(duration))
        if (durationString.empty) {
            return '0sec'
        } else {
            return durationString
        }
    }
}
