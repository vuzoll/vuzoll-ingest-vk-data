package com.github.vuzoll.ingestvk

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.queries.users.UserField
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class IngestVkController {

    static String DATA_FILE_PATH = '/data/vk.data'
    static Integer DEFAULT_SEED_ID = 3542756

    VkApiClient vk = new VkApiClient(HttpTransportClient.getInstance())
    File dataFile = new File(DATA_FILE_PATH)

    @RequestMapping(path = '/ingest', method = RequestMethod.POST)
    void ingest(@RequestBody IngestRequest ingestRequest) {
        int index = 0
        int startTime = System.currentTimeMillis()
        int ingestedCount = 0

        List<Integer> ids = readAllIds()
        if (ids.empty) {
            Integer seedId = ingestRequest.seedId ?: DEFAULT_SEED_ID
            VkProfile seed = ingestById(seedId)
            insertProfile seed
            ingestedCount++
            ids.add seedId
        }

        while (true) {
            if (index >= ids.size()) {
                return
            }

            if (ingestRequest.timeLimit && System.currentTimeMillis() >= startTime + ingestRequest.timeLimit) {
                return
            }

            if (ingestRequest.sizeLimit && ingestedCount >= ingestRequest.sizeLimit) {
                return
            }

            List<Integer> friendsIds = getFriendsIds(ids[index])
            friendsIds.collect({
                ids.add it
                ingestById it
            }).each({
                ingestedCount++
                insertProfile it
            })
        }
    }

    List<Integer> readAllIds() {
        if (!dataFile.exists()) {
            return []
        }

        List<Integer> ids = []
        dataFile.eachLine { String line ->
            ids.add VkProfile.readIdFromDataFileLine(line)
        }

        return ids
    }

    VkProfile ingestById(Integer id) {
        VkProfile.fromVkAPI vk.users()  .get()
                                        .userIds(id.toString())
                                        .fields(
                                            UserField.SEX, UserField.BDATE, UserField.CITY, UserField.COUNTRY, UserField.HOME_TOWN,
                                            UserField.EDUCATION, UserField.UNIVERSITIES, UserField.SCHOOLS, UserField.OCCUPATION, UserField.CAREER
                                        ).execute().get(0)
    }

    void insertProfile(VkProfile vkProfile) {
        dataFile.append vkProfile.toDataFileLine()
    }

    List<Integer> getFriendsIds(Integer id) {
        vk.friends().get()
                    .userId(id)
                    .execute()
                    .items
    }
}
