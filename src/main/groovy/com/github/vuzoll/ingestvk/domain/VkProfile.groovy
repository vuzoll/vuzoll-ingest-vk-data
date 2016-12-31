package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class VkProfile {

    Integer vkId
    String vkDomain
    Integer vkLastSeen
    Boolean vkActive

    Set<Integer> friendsIds

    Long ingestedTimestamp

    String birthday
    VkCity city
    VkCountry country
    String homeTown
    Integer sex

    VkOccupation occupation
    Set<VkCareerRecord> careerRecords
    Set<VkUniversityRecord> universityRecords
    Set<VkMilitaryRecord> militaryRecords
    Set<VkSchoolRecord> schoolRecords

    String skypeLogin
    String facebookId
    String facebookName
    String twitterId
    String livejournalId
    String instagramId
    String verified

    String about
    String activities
    String books
    String games
    String interests
    String movies
    String music
    VkPersonalBelief personalBelief
    String quotes
    Set<VkRelative> relatives
    Integer relationStatus
    String tvShows
}
