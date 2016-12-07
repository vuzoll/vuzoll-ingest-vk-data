package com.github.vuzoll.ingestvk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Country {

    Integer vkId

    static Country fromVkApi(com.vk.api.sdk.objects.base.Country vkApiCountry) {
        if (vkApiCountry) {
            return new Country(vkId: vkApiCountry.id)
        }
    }
}
