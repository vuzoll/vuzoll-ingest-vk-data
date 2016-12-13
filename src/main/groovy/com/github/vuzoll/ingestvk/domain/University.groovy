package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class University {

    Integer vkId
    String name
    City city
    Country country
}
