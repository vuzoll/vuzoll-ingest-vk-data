package com.github.vuzoll.ingestvk.domain

import com.vk.api.sdk.objects.users.UserXtrCounters
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = [ 'university', 'faculty' ])
class EducationRecord {

    University university
    Faculty faculty
    Integer graduationYear
}
