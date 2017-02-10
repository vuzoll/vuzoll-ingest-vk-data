package com.github.vuzoll.ingestvk.domain.vk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = [ 'universityId', 'facultyId', 'graduationYear' ])
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
