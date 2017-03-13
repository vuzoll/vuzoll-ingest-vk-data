package com.github.vuzoll.ingestvk.domain

import groovy.transform.EqualsAndHashCode
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed

@EqualsAndHashCode(includes = 'vkId')
class VkProfile {

    @Id
    String id

    @Indexed
    Integer vkId

    @Indexed
    Integer ingestionIndex

    @Indexed
    String datasetName

    String vkDomain
    Integer vkLastSeen
    Boolean vkActive

    String firstName
    String lastName
    String maidenName
    String middleName
    String mobilePhone
    String homePhone
    VkRelationPartner relationPartner

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
    String screenName
    String site

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
