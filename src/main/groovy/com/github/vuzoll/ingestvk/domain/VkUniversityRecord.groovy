package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode
import org.springframework.data.annotation.Id

@EqualsAndHashCode(includes = [ 'universityId', 'facultyId', 'graduationYear' ])
class VkUniversityRecord {

    @Id
    String id

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
