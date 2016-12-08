package com.github.vuzoll.ingestvk

import com.vk.api.sdk.objects.base.BaseObject
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class City {

    Integer vkId
    String name
    Country country

    static City fromVkApi(BaseObject vkApiCity, com.vk.api.sdk.objects.base.Country vkApiCountry) {
        if (vkApiCity) {
            new City(vkId: vkApiCity.id, name: vkApiCity.title, country: Country.fromVkApi(vkApiCountry))
        } else {
            null
        }
    }
}
