package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = [ 'vkId', 'classLetter', 'yearFrom', 'yearTo' ])
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
