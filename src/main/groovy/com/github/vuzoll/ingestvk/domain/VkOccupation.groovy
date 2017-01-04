package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode
import org.springframework.data.annotation.Id

@EqualsAndHashCode
class VkOccupation {

    @Id
    String id

    Integer vkId
    String type
    String name
}
