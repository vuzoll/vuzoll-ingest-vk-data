package com.github.vuzoll.ingestvk.domain.vk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class VkCity {

    Integer vkId
    String name
}
