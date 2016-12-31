package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class VkProfile {

    Integer vkId
    String vkDomain
    Integer vkLastSeen

    Collection<Integer> friendsIds

    Long ingestedTimestamp

    String birthday
    VkCity city
    VkCountry country
    String homeTown
    Integer sex

    VkOccupation occupation
    Collection<VkCareerRecord> careerRecords
    Collection<VkUniversityRecord> universityRecords
    Collection<VkMilitaryRecord> militaryRecords
    Collection<VkSchoolRecord> schoolRecords

    String about
    String activities
    String books
    String games
    String interests
    String movies
    String music
    VkPersonalBelief personalBelief
    String quotes
    Collection<VkRelative> relatives
    Integer relationStatus
    String tvShows
}
