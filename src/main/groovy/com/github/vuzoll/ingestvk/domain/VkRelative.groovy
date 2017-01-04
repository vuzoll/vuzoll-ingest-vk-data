package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode
import org.springframework.data.annotation.Id

@EqualsAndHashCode(includes = 'vkId')
class VkRelative {

    @Id
    String id

    Integer vkId
    String type
}
