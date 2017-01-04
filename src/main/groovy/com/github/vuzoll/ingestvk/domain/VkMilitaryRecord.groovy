package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode
import org.springframework.data.annotation.Id

@EqualsAndHashCode
class VkMilitaryRecord {

    @Id
    String id

    Integer vkId
    String unit
    Integer countryId
    Integer from
    Integer until
}
