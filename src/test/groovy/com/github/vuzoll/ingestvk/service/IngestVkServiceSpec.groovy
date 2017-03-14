package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.ingestvk.domain.VkProfile
import com.vk.api.sdk.objects.users.UserFull
import com.vk.api.sdk.objects.users.UserMin
import spock.lang.Specification

class IngestVkServiceSpec extends Specification {

    def 'Collection<VkProfile> profilesToSave(Closure statusUpdater, Collection<Integer> idsToSave): happy-path scenario'() {
        setup:
        String datasetName = 'datasetName'

        String seedGroupId1 = 'seed-group-1'
        Collection<String> seedGroupIds = [ seedGroupId1 ]

        Integer universityId1 = 28
        Collection<Integer> universityIdsToAccept = [ universityId1 ]

        Integer idToSave1 = 17
        Collection<Integer> idsToSave = [ idToSave1 ]

        Integer userId1 = 1
        Integer userId2 = 2

        UserFull vkUser = new UserFull()
        def userIdField = UserMin.getDeclaredField('id')
        userIdField.setAccessible(true)
        userIdField.set(vkUser, userId1)
        vkUser.university = universityId1

        VkApiService vkApiService = Mock()
        vkApiService.ingestVkProfilesById(_) >> [ vkUser ]
        vkApiService.getFriendsIds(userId1) >> [ userId2 ]

        IngestVkService ingestVkService = new IngestVkService()
        ingestVkService.vkApiService = vkApiService

        IngestVkService.BasicIngestJob basicIngestJob = ingestVkService.ingestUsingGroupBfsJob(datasetName, seedGroupIds, universityIdsToAccept)

        when:
        Collection<VkProfile> profilesToSave = basicIngestJob.profilesToSave(simpleStatusUpdater(), idsToSave)

        then:
        noExceptionThrown()

        profilesToSave.size() == 1
        with(profilesToSave[0]) {
            vkId == userId1
            datasetName == datasetName
            friendsIds.size() == 1
            friendsIds[0] == userId2

            universityRecords.size() == 1
            with(universityRecords[0]) {
                universityId == universityId1
            }
        }
    }

    private Closure simpleStatusUpdater() {
        return { println it.message }
    }
}
