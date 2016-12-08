package com.github.vuzoll.ingestvk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class University {

    Integer vkId
    String name

    static University fromVkId(Integer vkId, String name) {
        if (vkId) {
            return new University(vkId: vkId, name: name)
        } else {
            null
        }
    }
}
