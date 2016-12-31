package com.github.vuzoll.ingestvk.domain

import com.vk.api.sdk.objects.users.UserXtrCounters
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class VkUniversityRecord {

    Integer universityId
    Integer countryId
    Integer cityId
    String universityName
    Integer facultyId
    String facultyName
    Integer chairId
    String chairName
    Integer graduationYear
    String educationForm
    String educationStatus
}
