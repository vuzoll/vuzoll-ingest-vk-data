package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode
import org.springframework.data.annotation.Id

@EqualsAndHashCode
class VkCareerRecord {

    @Id
    String id

    Integer groupId
    Integer countryId
    Integer cityId
    Integer from
    Integer until
    String position
}
