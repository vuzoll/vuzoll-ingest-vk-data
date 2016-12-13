package com.github.vuzoll.ingestvk.domain

import com.vk.api.sdk.objects.base.BaseObject
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class City {

    Integer vkId
    String name
    Country country
}
