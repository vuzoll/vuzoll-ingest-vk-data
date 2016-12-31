package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class VkMilitaryRecord {

    Integer vkId
    String unit
    Integer countryId
    Integer from
    Integer until
}
