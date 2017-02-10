package com.github.vuzoll.ingestvk.domain.vk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class VkRelationPartner {

    Integer vkId
    String firstName
    String lastName
}
