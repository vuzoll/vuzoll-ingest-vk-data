package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.ingestvk.domain.vk.VkCity
import com.github.vuzoll.ingestvk.domain.vk.VkCountry
import com.github.vuzoll.ingestvk.domain.vk.VkRelationPartner
import com.github.vuzoll.ingestvk.domain.vk.VkSchoolRecord
import com.github.vuzoll.ingestvk.domain.vk.VkUniversityRecord

import com.github.vuzoll.ingestvk.domain.vk.VkCareerRecord
import com.github.vuzoll.ingestvk.domain.vk.VkMilitaryRecord
import com.github.vuzoll.ingestvk.domain.vk.VkOccupation
import com.github.vuzoll.ingestvk.domain.vk.VkPersonalBelief
import com.github.vuzoll.ingestvk.domain.vk.VkProfile
import com.github.vuzoll.ingestvk.domain.vk.VkRelative
import com.vk.api.sdk.client.Lang
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.exceptions.ApiAuthValidationException
import com.vk.api.sdk.exceptions.ApiUserDeletedException
import com.vk.api.sdk.httpclient.HttpTransportClient
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
import com.vk.api.sdk.queries.users.UserField
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@Service
@Slf4j
class VkApiService {

    static Integer VK_USER_ID = System.getenv('INGEST_VK_USER_ID') ? Integer.parseInt(System.getenv('INGEST_VK_USER_ID')) : null
    static String VK_ACCESS_TOKEN = System.getenv('INGEST_VK_ACCESS_TOKEN')

    static long VK_API_REQUEST_DELAY = 350

    VkApiClient vk = new VkApiClient(HttpTransportClient.getInstance())

    VkProfile ingestVkProfileById(Integer id) {
        try {
            log.debug "Ingesting vk profile by id=$id..."
            Thread.sleep(VK_API_REQUEST_DELAY)

            def vkRequest
            if (VK_USER_ID && VK_ACCESS_TOKEN) {
                vkRequest = vk.users().get(new UserActor(VK_USER_ID, VK_ACCESS_TOKEN))
            } else {
                vkRequest = vk.users().get()
            }

            UserFull vkApiUser = vkRequest
                    .userIds(id.toString())
                    .fields(
                    UserField.ABOUT,     UserField.ACTIVITIES, UserField.BDATE,        UserField.BOOKS,
                    UserField.CAREER,    UserField.CITY,       UserField.CONNECTIONS,  UserField.CONTACTS,
                    UserField.COUNTRY,   UserField.DOMAIN,     UserField.EDUCATION,    UserField.GAMES,
                    UserField.HOME_TOWN, UserField.INTERESTS,  UserField.LAST_SEEN,    UserField.MILITARY,
                    UserField.MOVIES,    UserField.MUSIC,      UserField.OCCUPATION,   UserField.PERSONAL,
                    UserField.QUOTES,    UserField.RELATIVES,  UserField.RELATION,     UserField.SCHOOLS,
                    UserField.SEX,       UserField.TV,         UserField.UNIVERSITIES, UserField.VERIFIED
            )
                    .lang(Lang.UA)
                    .execute().get(0)

            return toVkProfile(vkApiUser)
        } catch (ApiAuthValidationException e) {
            throw new RuntimeException("vk validation required - visit $e.redirectUri", e)
        }
    }

    Collection<Integer> getFriendsIds(Integer id) {
        try {
            log.debug "Getting friend list of profile id=$id..."
            Thread.sleep(VK_API_REQUEST_DELAY)

            def vkRequest
            if (VK_USER_ID && VK_ACCESS_TOKEN) {
                vkRequest = vk.friends().get(new UserActor(VK_USER_ID, VK_ACCESS_TOKEN))
            } else {
                vkRequest = vk.friends().get()
            }

            return vkRequest
                    .userId(id)
                    .execute()
                    .items
        } catch (ApiAuthValidationException e) {
            log.error("vk validation required - visit $e.redirectUri", e)
            throw new RuntimeException("vk validation required - visit $e.redirectUri", e)
        } catch (ApiUserDeletedException e) {
            log.warn("User with id=$id was deactivated", e)
            return []
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

        vkProfile.friendsIds = getFriendsIds(vkApiUser.id)

        vkProfile.ingestedTimestamp = System.currentTimeMillis()

        vkProfile.birthday = vkApiUser.bdate
        vkProfile.city = toVkCity(vkApiUser.city)
        vkProfile.country = toVkCountry(vkApiUser.country)
        vkProfile.homeTown = vkApiUser.homeTown
        vkProfile.sex = vkApiUser.sex

        vkProfile.occupation = toVkOccupation(vkApiUser.occupation)
        vkProfile.careerRecords = vkApiUser.career?.collect(this.&toVkCareerRecord)?.findAll({ it != null }) ?: []
        vkProfile.universityRecords = ((vkApiUser.universities?.collect(this.&toVkUniversityRecord) ?: []) + toVkUniversityRecord(vkApiUser))?.findAll({ it != null }) ?: []
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
}