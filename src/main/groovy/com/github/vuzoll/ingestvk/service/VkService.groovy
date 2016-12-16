package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.ingestvk.domain.City
import com.github.vuzoll.ingestvk.domain.Country
import com.github.vuzoll.ingestvk.domain.EducationRecord
import com.github.vuzoll.ingestvk.domain.Faculty
import com.github.vuzoll.ingestvk.domain.University
import com.github.vuzoll.ingestvk.domain.VkProfile
import com.vk.api.sdk.client.Lang
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.objects.base.BaseObject
import com.vk.api.sdk.objects.users.UserFull
import com.vk.api.sdk.queries.users.UserField
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@Service
@Slf4j
class VkService {

    static Integer VK_USER_ID = System.getenv('INGEST_VK_USER_ID') ? Integer.parseInt(System.getenv('INGEST_VK_USER_ID')) : null
    static String VK_ACCESS_TOKEN = System.getenv('INGEST_VK_ACCESS_TOKEN')

    static long VK_API_REQUEST_DELAY = 350

    static Map<Integer, City> CITY_CACHE = [:]
    static Map<Integer, Country> COUNTRY_CACHE = [:]

    VkApiClient vk = new VkApiClient(HttpTransportClient.getInstance())

    VkProfile ingestVkUserById(Integer id) {
        log.debug "Loading profile id:$id..."
        Thread.sleep(VK_API_REQUEST_DELAY)

        def vkRequest
        if (VK_USER_ID && VK_ACCESS_TOKEN) {
            vkRequest = vk.users().get(new UserActor(VK_USER_ID, VK_ACCESS_TOKEN))
        } else {
            vkRequest = vk.users().get()
        }

        UserFull vkApiUser = vkRequest
                .userIds(id.toString())
                .fields(UserField.CITY, UserField.COUNTRY, UserField.EDUCATION, UserField.UNIVERSITIES)
                .lang(Lang.UA)
                .execute().get(0)

        return toVkProfile(vkApiUser)
    }

    Collection<Integer> searchStudentIdsFromCountry(Country country, int offset, int count) {
        log.debug "Loading $count ids of students from country id:$country.vkId starting from index $offset..."
        Thread.sleep(VK_API_REQUEST_DELAY)

        vk.users().search(new UserActor(VK_USER_ID, VK_ACCESS_TOKEN))
                .universityCountry(country.vkId)
                .offset(offset)
                .count(count)
                .execute().items.collect({ it.id })
    }

    int countStudentsFromCountry(Country country) {
        log.debug "Counting students from country id:$country.vkId..."
        Thread.sleep(VK_API_REQUEST_DELAY)

        vk.users().search(new UserActor(VK_USER_ID, VK_ACCESS_TOKEN))
                .universityCountry(country.vkId)
                .execute().count
    }

    private VkProfile toVkProfile(UserFull vkApiUser) {
        VkProfile vkProfile = new VkProfile()
        vkProfile.vkId = vkApiUser.id
        vkProfile.name = "$vkApiUser.firstName $vkApiUser.lastName"
        vkProfile.city = createCity(vkApiUser.city, vkApiUser.country)
        vkProfile.country = createCountry(vkApiUser.country)
        vkProfile.educationRecords = ([ createEducationRecord(vkApiUser) ] + vkApiUser.universities.collect({ createEducationRecord(it) })).findAll { it != null }

        return vkProfile
    }

    List<Integer> getFriendsIds(Integer id) {
        log.debug "Getting friend list of profile id:$id..."
        Thread.sleep(VK_API_REQUEST_DELAY)

        try {
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
        } catch (e) {
            log.warn("Failed to get friend list of profile id:$id", e)
            return []
        }
    }

    City createCity(BaseObject vkApiCity, com.vk.api.sdk.objects.base.Country vkApiCountry) {
        if (vkApiCity) {
            if (CITY_CACHE.containsKey(vkApiCity.id)) {
                return CITY_CACHE[vkApiCity.id]
            } else {
                City newInstance = new City(vkId: vkApiCity.id, name: vkApiCity.title, country: createCountry(vkApiCountry))
                CITY_CACHE[vkApiCity.id] = newInstance
                return newInstance
            }
        } else {
            return null
        }
    }

    Country createCountry(com.vk.api.sdk.objects.base.Country vkApiCountry) {
        if (vkApiCountry) {
            if (COUNTRY_CACHE.containsKey(vkApiCountry.id)) {
                return COUNTRY_CACHE[vkApiCountry.id]
            } else {
                Country newInstance = new Country(vkId: vkApiCountry.id, name: vkApiCountry.title)
                COUNTRY_CACHE[vkApiCountry.id] = newInstance
                return newInstance
            }
        } else {
            return null
        }
    }

    EducationRecord createEducationRecord(UserFull vkApiUser) {
        if (vkApiUser.university || vkApiUser.faculty) {
            new EducationRecord(university: getUniversity(vkApiUser.university, vkApiUser.universityName), faculty: getFaculty(vkApiUser.faculty, vkApiUser.facultyName, vkApiUser.university, vkApiUser.universityName), graduationYear: vkApiUser.graduation)
        } else {
            null
        }
    }

    EducationRecord createEducationRecord(com.vk.api.sdk.objects.users.University university) {
        if (university.id || university.faculty) {
            new EducationRecord(university: getUniversity(university.id, university.name), faculty: getFaculty(university.faculty, university.facultyName, university.id, university.name), graduationYear: university.graduation)
        } else {
            null
        }
    }

    @Memoized
    Faculty getFaculty(Integer vkId, String name, Integer universityVkId, String universityName) {
        if (vkId) {
            return new Faculty(vkId: vkId, name: name, university: getUniversity(universityVkId, universityName))
        } else {
            null
        }
    }

    @Memoized
    University getUniversity(Integer vkId, String name) {
        if (vkId) {
            Country universityCounty = guessUniversityCounty(vkId, name)
            City universityCity = guessUniversityCity(vkId, name, universityCounty)
            return new University(vkId: vkId, name: name, city: universityCity, country: universityCounty)
        } else {
            null
        }
    }

    @Memoized
    Country guessUniversityCounty(Integer universityId, String universityName) {
        if (isUniversityInCountry(universityId, universityName, 2)) {
            return getCountry(2)
        } else {
            return null
        }
    }

    @Memoized
    Country getCountry(Integer id) {
        log.debug "Getting country with id:$id..."
        Thread.sleep(VK_API_REQUEST_DELAY)

        return createCountry(vk.database().countriesById.countryIds(id).execute()[0])
    }

    @Memoized
    boolean isUniversityInCountry(Integer universityId, String universityName, Integer countryId) {
        log.debug "Checking if university id:$universityId is located in country id:$countryId"
        Thread.sleep(VK_API_REQUEST_DELAY)

        try {
            String query = universityName.contains(' ') ? universityName.substring(0, universityName.indexOf(' ')) : universityName
            return vk.database().universities.countryId(countryId).lang(Lang.UA).q(query).execute().items.find({ it.id == universityId }) != null
        } catch (e) {
            log.warn("Failed checking if university id:$universityId is located in country id:$countryId", e)
            return false
        }
    }

    @Memoized
    City guessUniversityCity(Integer universityId, String universityName, Country country) {
        if (country) {
            Integer cityId = streamCitiesIdByCountry(country.vkId, 1000, 0).find({ isUniversityInCity(universityId, universityName, it) })
            if (cityId) {
                return getCity(cityId, country.vkId)
            } else {
                return null
            }
        } else {
            return null
        }
    }

    @Memoized
    City getCity(Integer cityId, Integer countryId) {
        log.debug "Getting city with id:$cityId..."
        Thread.sleep(2 * VK_API_REQUEST_DELAY)

        return createCity(vk.database().citiesById.cityIds(cityId).execute()[0], vk.database().countriesById.countryIds(countryId).execute()[0])
    }

    @Memoized
    boolean isUniversityInCity(Integer universityId, String universityName, Integer cityId) {
        log.debug "Checking if university id:$universityId is located in city id:$cityId"
        Thread.sleep(VK_API_REQUEST_DELAY)

        try {
            String query = universityName.contains(' ') ? universityName.substring(0, universityName.indexOf(' ')) : universityName
            return vk.database().universities.cityId(cityId).lang(Lang.UA).q(query).execute().items.find({ it.id == universityId }) != null
        } catch (e) {
            log.warn("Failed checking if university id:$universityId is located in city id:$cityId", e)
            return false
        }
    }

    @Memoized
    List<Integer> streamCitiesIdByCountry(Integer countryId, Integer batchSize, Integer offset) {
        log.debug "Streaming $batchSize of cities (starting from $offset) located in country with id:$countryId..."
        Thread.sleep(VK_API_REQUEST_DELAY)

        return vk.database().getCities(countryId).count(batchSize).offset(offset).execute().items.id
    }
}
