package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode
import org.springframework.data.annotation.Id

@EqualsAndHashCode
class VkPersonalBelief {

    @Id
    String id

    Integer politicalBelief
    Collection<String> languages
    String religionBelief
    String inspiredBy
    Integer importantInPeople
    Integer importantInLife
    Integer smokingAttitude
    Integer alcoholAttitude
}
