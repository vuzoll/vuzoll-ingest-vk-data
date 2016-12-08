package com.github.vuzoll.ingestvk

import com.vk.api.sdk.objects.users.UserXtrCounters
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = [ 'university', 'faculty' ])
class EducationRecord {

    University university
    Faculty faculty
    Integer graduationYear

    static EducationRecord fromVkApi(UserXtrCounters vkApiUser) {
        if (vkApiUser.university || vkApiUser.faculty) {
            new EducationRecord(university: University.fromVkId(vkApiUser.university, vkApiUser.universityName), faculty: Faculty.fromVkId(vkApiUser.faculty, vkApiUser.facultyName, vkApiUser.university, vkApiUser.universityName), graduationYear: vkApiUser.graduation)
        } else {
            null
        }
    }

    static EducationRecord fromVkApi(com.vk.api.sdk.objects.users.University university) {
        if (university.id || university.faculty) {
            new EducationRecord(university: University.fromVkId(university.id, university.name), faculty: Faculty.fromVkId(university.faculty, university.facultyName, university.id, university.name), graduationYear: university.graduation)
        } else {
            null
        }
    }
}
