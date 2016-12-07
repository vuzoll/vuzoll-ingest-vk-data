package com.github.vuzoll.ingestvk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class University {

    Integer vkId

    static University fromVkId(Integer vkId) {
        if (vkId) {
            return new University(vkId: vkId)
        } else {
            null
        }
    }
}
