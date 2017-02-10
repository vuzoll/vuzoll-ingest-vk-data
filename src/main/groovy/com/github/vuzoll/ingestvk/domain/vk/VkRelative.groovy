package com.github.vuzoll.ingestvk.domain.vk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class VkRelative {

    Integer vkId
    String type
}
