package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class VkRelationPartner {

    Integer vkId
    String firstName
    String lastName
}
