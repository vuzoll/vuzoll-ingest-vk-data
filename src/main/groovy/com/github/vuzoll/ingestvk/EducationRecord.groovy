package com.github.vuzoll.ingestvk

import com.vk.api.sdk.objects.users.UserXtrCounters
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class EducationRecord {

    University university
    Faculty faculty
    Integer graduationYear

    static EducationRecord fromVkApi(UserXtrCounters vkApiUser) {
        if (vkApiUser.university || vkApiUser.faculty) {
            new EducationRecord(university: University.fromVkId(vkApiUser.university), faculty: Faculty.fromVkId(vkApiUser.faculty), graduationYear: vkApiUser.graduation)
        } else {
            null
        }
    }

    static EducationRecord fromVkApi(com.vk.api.sdk.objects.users.University university) {
        if (university.id || university.faculty) {
            new EducationRecord(university: University.fromVkId(university.id), faculty: Faculty.fromVkId(university.faculty), graduationYear: university.graduation)
        } else {
            null
        }
    }
}
