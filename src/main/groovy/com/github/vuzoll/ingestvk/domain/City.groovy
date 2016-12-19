package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class City {

    Integer vkId
    String name
    Country country
}
