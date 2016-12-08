package com.github.vuzoll.ingestvk

import com.vk.api.sdk.objects.users.UserXtrCounters
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class VkProfile {

    Integer vkId
    String name
    City city
    Country country
    Set<EducationRecord> educationRecords

    static VkProfile fromVkAPI(UserXtrCounters vkApiUser) {
        VkProfile vkProfile = new VkProfile()
        vkProfile.vkId = vkApiUser.id
        vkProfile.name = "$vkApiUser.firstName $vkApiUser.lastName"
        vkProfile.city = City.fromVkApi(vkApiUser.city, vkApiUser.country)
        vkProfile.country = Country.fromVkApi(vkApiUser.country)
        vkProfile.educationRecords = ([ EducationRecord.fromVkApi(vkApiUser) ] + vkApiUser.universities.collect({ EducationRecord.fromVkApi(it) })).findAll { it != null }

        return vkProfile
    }
}
