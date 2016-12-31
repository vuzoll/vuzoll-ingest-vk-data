package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class VkSchoolRecord {

    String vkId
    Integer countryId
    Integer cityId
    String name
    Integer yearFrom
    Integer yearTo
    Integer graduationYear
    String classLetter
    Integer typeId
    String typeName
}
