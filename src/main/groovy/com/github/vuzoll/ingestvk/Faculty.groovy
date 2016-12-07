package com.github.vuzoll.ingestvk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Faculty {

    Integer vkId

    static Faculty fromVkId(Integer vkId) {
        if (vkId) {
            return new Faculty(vkId: vkId)
        } else {
            null
        }
    }
}
