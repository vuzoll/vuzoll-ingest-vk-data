package com.github.vuzoll.ingestvk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class Country {

    Integer vkId
    String name

    static Country fromVkApi(com.vk.api.sdk.objects.base.Country vkApiCountry) {
        if (vkApiCountry) {
            return new Country(vkId: vkApiCountry.id, name: vkApiCountry.title)
        }
    }
}
