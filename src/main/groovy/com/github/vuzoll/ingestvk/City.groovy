package com.github.vuzoll.ingestvk

import com.vk.api.sdk.objects.base.BaseObject
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class City {

    Integer vkId

    static City fromVkApi(BaseObject vkApiCity) {
        if (vkApiCity) {
            new City(vkId: vkApiCity.id)
        } else {
            null
        }
    }
}
