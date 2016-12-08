package com.github.vuzoll.ingestvk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class Faculty {

    Integer vkId
    String name
    University university

    static Faculty fromVkId(Integer vkId, String name, Integer universityVkId, String universityName) {
        if (vkId) {
            return new Faculty(vkId: vkId, name: name, university: University.fromVkId(universityVkId, universityName))
        } else {
            null
        }
    }
}
