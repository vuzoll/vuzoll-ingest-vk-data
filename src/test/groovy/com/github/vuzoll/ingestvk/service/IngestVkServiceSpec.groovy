package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.ingestvk.domain.VkProfile
import com.github.vuzoll.ingestvk.repository.VkProfileRepository
import com.vk.api.sdk.objects.base.Sex
import com.vk.api.sdk.objects.users.University
import com.vk.api.sdk.objects.users.User
import com.vk.api.sdk.objects.users.UserFull
import com.vk.api.sdk.objects.users.UserMin
import spock.lang.Specification

import java.lang.reflect.Field

class IngestVkServiceSpec extends Specification {

    def 'Collection<VkProfile> profilesToSave(Closure statusUpdater, Collection<Integer> idsToSave): with university record'() {
        setup:
        String datasetName = 'datasetName'

        String seedGroupId1 = 'seed-group-1'
        Collection<String> seedGroupIds = [ seedGroupId1 ]

        Integer universityId1 = 28
        Integer universityId2 = 1128
        Collection<Integer> universityIdsToAccept = [ universityId1, universityId2 ]

        Integer idToSave1 = 17
        Collection<Integer> idsToSave = [ idToSave1 ]

        Integer userId1 = 1
        Integer userId2 = 2

        UserFull vkUser = new UserFull()
        setProtectedField(UserMin.getDeclaredField('id'), vkUser, userId1)
        setProtectedField(User.getDeclaredField('sex'), vkUser, Sex.MALE)
        vkUser.university = universityId1

        University university2 = new University()
        setProtectedField(University.getDeclaredField('id'), university2, universityId2)
        vkUser.universities = [ university2 ]

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
            sex == 'MALE'
            friendsIds.size() == 1
            friendsIds[0] == userId2

            universityRecords.size() == 1
            with(universityRecords[0]) {
                universityId == universityId2
            }
        }
    }

    def 'Collection<VkProfile> profilesToSave(Closure statusUpdater, Collection<Integer> idsToSave): without university record'() {
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
        setProtectedField(UserMin.getDeclaredField('id'), vkUser, userId1)
        setProtectedField(User.getDeclaredField('sex'), vkUser, Sex.MALE)
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
            sex == 'MALE'
            friendsIds.size() == 1
            friendsIds[0] == userId2

            universityRecords.size() == 1
            with(universityRecords[0]) {
                universityId == universityId1
            }
        }
    }

    def 'DurableJob ingestUsingGroupBfsJob(String datasetName, Collection<String> seedGroupIds, Collection<Integer> universityIdsToAccept): ingestionIndexForNextRecord and indexOfRecordToIngestNext are used as expected when dataset is initially empty'() {
        setup:
        String datasetName = 'datasetName'

        String seedGroupId1 = 'seed-group-1'
        Collection<String> seedGroupIds = [ seedGroupId1 ]

        Integer universityId1 = 28
        Integer universityId2 = 1128
        Collection<Integer> universityIdsToAccept = [ universityId1, universityId2 ]

        VkProfile vkProfile = new VkProfile()

        VkProfileRepository vkProfileRepository = Mock()
        vkProfileRepository.findOneByDatasetNameAndIngestionIndex(_, _) >> vkProfile

        IngestVkService ingestVkService = new IngestVkService()
        ingestVkService.vkProfileRepository = vkProfileRepository

        IngestVkService.BasicIngestJob ingestUsingGroupBfsJob = ingestVkService.ingestUsingGroupBfsJob(datasetName, seedGroupIds, universityIdsToAccept)

        expect:
        ingestUsingGroupBfsJob.indexOfRecordToIngestNext == 0

        when:
        vkProfile = ingestUsingGroupBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 0

        when:
        vkProfile = ingestUsingGroupBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 1

        when:
        ingestUsingGroupBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingGroupBfsJob.indexOfRecordToIngestNext == 1

        when:
        ingestUsingGroupBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingGroupBfsJob.indexOfRecordToIngestNext == 2

        when:
        ingestUsingGroupBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingGroupBfsJob.indexOfRecordToIngestNext == 3

        when:
        vkProfile = ingestUsingGroupBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 2

        when:
        vkProfile = ingestUsingGroupBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 3
    }

    def 'DurableJob ingestUsingGroupBfsJob(String datasetName, Collection<String> seedGroupIds, Collection<Integer> universityIdsToAccept): ingestionIndexForNextRecord and indexOfRecordToIngestNext are used as expected when dataset is initially non-empty'() {
        setup:
        String datasetName = 'datasetName'

        String seedGroupId1 = 'seed-group-1'
        Collection<String> seedGroupIds = [ seedGroupId1 ]

        Integer universityId1 = 28
        Integer universityId2 = 1128
        Collection<Integer> universityIdsToAccept = [ universityId1, universityId2 ]

        VkProfile vkProfile = new VkProfile()

        VkProfileRepository vkProfileRepository = Mock()
        vkProfileRepository.countByDatasetName(datasetName) >> 5
        vkProfileRepository.findOneByDatasetNameAndIngestionIndex(_, _) >> vkProfile

        IngestVkService ingestVkService = new IngestVkService()
        ingestVkService.vkProfileRepository = vkProfileRepository

        IngestVkService.BasicIngestJob ingestUsingGroupBfsJob = ingestVkService.ingestUsingGroupBfsJob(datasetName, seedGroupIds, universityIdsToAccept)

        when:
        ingestUsingGroupBfsJob.initSelf(simpleStatusUpdater())

        then:
        ingestUsingGroupBfsJob.indexOfRecordToIngestNext == 0

        when:
        vkProfile = ingestUsingGroupBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 5

        when:
        vkProfile = ingestUsingGroupBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 6

        when:
        ingestUsingGroupBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingGroupBfsJob.indexOfRecordToIngestNext == 1

        when:
        ingestUsingGroupBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingGroupBfsJob.indexOfRecordToIngestNext == 2

        when:
        ingestUsingGroupBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingGroupBfsJob.indexOfRecordToIngestNext == 3

        when:
        vkProfile = ingestUsingGroupBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 7

        when:
        vkProfile = ingestUsingGroupBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 8
    }

    def 'DurableJob ingestUsingBfsJob(String datasetName, Collection<String> seedGroupIds, Collection<Integer> universityIdsToAccept): ingestionIndexForNextRecord and indexOfRecordToIngestNext are used as expected when dataset is initially empty'() {
        setup:
        String datasetName = 'datasetName'

        Integer seedId = 28

        VkProfile vkProfile = new VkProfile()

        VkProfileRepository vkProfileRepository = Mock()
        vkProfileRepository.findOneByDatasetNameAndIngestionIndex(_, _) >> vkProfile

        IngestVkService ingestVkService = new IngestVkService()
        ingestVkService.vkProfileRepository = vkProfileRepository

        IngestVkService.BasicIngestJob ingestUsingBfsJob = ingestVkService.ingestUsingBfsJob(datasetName, seedId)

        expect:
        ingestUsingBfsJob.indexOfRecordToIngestNext == 0

        when:
        vkProfile = ingestUsingBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 0

        when:
        vkProfile = ingestUsingBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 1

        when:
        ingestUsingBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingBfsJob.indexOfRecordToIngestNext == 1

        when:
        ingestUsingBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingBfsJob.indexOfRecordToIngestNext == 2

        when:
        ingestUsingBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingBfsJob.indexOfRecordToIngestNext == 3

        when:
        vkProfile = ingestUsingBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 2

        when:
        vkProfile = ingestUsingBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 3
    }

    def 'DurableJob ingestUsingBfsJob(String datasetName, Collection<String> seedGroupIds, Collection<Integer> universityIdsToAccept): ingestionIndexForNextRecord and indexOfRecordToIngestNext are used as expected when dataset is initially non-empty'() {
        setup:
        String datasetName = 'datasetName'

        Integer seedId = 28

        VkProfile vkProfile = new VkProfile()

        VkProfileRepository vkProfileRepository = Mock()
        vkProfileRepository.countByDatasetName(datasetName) >> 5
        vkProfileRepository.findOneByDatasetNameAndIngestionIndex(_, _) >> vkProfile

        IngestVkService ingestVkService = new IngestVkService()
        ingestVkService.vkProfileRepository = vkProfileRepository

        IngestVkService.BasicIngestJob ingestUsingBfsJob = ingestVkService.ingestUsingBfsJob(datasetName, seedId)

        when:
        ingestUsingBfsJob.initSelf(simpleStatusUpdater())

        then:
        ingestUsingBfsJob.indexOfRecordToIngestNext == 0

        when:
        vkProfile = ingestUsingBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 5

        when:
        vkProfile = ingestUsingBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 6

        when:
        ingestUsingBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingBfsJob.indexOfRecordToIngestNext == 1

        when:
        ingestUsingBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingBfsJob.indexOfRecordToIngestNext == 2

        when:
        ingestUsingBfsJob.getNextProfileToIngest(simpleStatusUpdater())

        then:
        ingestUsingBfsJob.indexOfRecordToIngestNext == 3

        when:
        vkProfile = ingestUsingBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 7

        when:
        vkProfile = ingestUsingBfsJob.processProfile(simpleStatusUpdater(), vkProfile)

        then:
        vkProfile.ingestionIndex == 8
    }

    def 'DurableJob ingestUsingBfsJob(String datasetName, Collection<String> seedGroupIds, Collection<Integer> universityIdsToAccept): finished works as expected'() {
        setup:
        String datasetName = 'datasetName'

        Integer seedId = 28

        VkProfileRepository vkProfileRepository = Mock()
        vkProfileRepository.countByDatasetName(datasetName) >> 5

        IngestVkService ingestVkService = new IngestVkService()
        ingestVkService.vkProfileRepository = vkProfileRepository

        IngestVkService.BasicIngestJob ingestUsingBfsJob = ingestVkService.ingestUsingBfsJob(datasetName, seedId)

        when:
        ingestUsingBfsJob.indexOfRecordToIngestNext = 4

        then:
        ingestUsingBfsJob.finished(simpleStatusUpdater()) == false

        when:
        ingestUsingBfsJob.indexOfRecordToIngestNext = 5

        then:
        ingestUsingBfsJob.finished(simpleStatusUpdater()) == true

        when:
        ingestUsingBfsJob.indexOfRecordToIngestNext = 6

        then:
        ingestUsingBfsJob.finished(simpleStatusUpdater()) == true
    }

    def 'DurableJob ingestUsingGroupBfsJob(String datasetName, Collection<String> seedGroupIds, Collection<Integer> universityIdsToAccept): finished works as expected'() {
        setup:
        String datasetName = 'datasetName'

        String seedGroupId1 = 'seed-group-1'
        Collection<String> seedGroupIds = [ seedGroupId1 ]

        Integer universityId1 = 28
        Integer universityId2 = 1128
        Collection<Integer> universityIdsToAccept = [ universityId1, universityId2 ]

        VkProfileRepository vkProfileRepository = Mock()
        vkProfileRepository.countByDatasetName(datasetName) >> 5

        IngestVkService ingestVkService = new IngestVkService()
        ingestVkService.vkProfileRepository = vkProfileRepository

        IngestVkService.BasicIngestJob ingestUsingBfsJob = ingestVkService.ingestUsingGroupBfsJob(datasetName, seedGroupIds, universityIdsToAccept)

        when:
        ingestUsingBfsJob.indexOfRecordToIngestNext = 4

        then:
        ingestUsingBfsJob.finished(simpleStatusUpdater()) == false

        when:
        ingestUsingBfsJob.indexOfRecordToIngestNext = 5

        then:
        ingestUsingBfsJob.finished(simpleStatusUpdater()) == true

        when:
        ingestUsingBfsJob.indexOfRecordToIngestNext = 6

        then:
        ingestUsingBfsJob.finished(simpleStatusUpdater()) == true
    }

    void setProtectedField(Field field, object, value) {
        field.setAccessible(true)
        field.set(object, value)
    }

    private Closure simpleStatusUpdater() {
        return { println it.message }
    }
}
