package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode
import org.springframework.data.annotation.Id

@EqualsAndHashCode(includes = 'vkId')
class VkCountry {

    @Id
    String id

    Integer vkId
    String name
}
