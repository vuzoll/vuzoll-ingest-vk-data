package com.github.vuzoll.ingestvk

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.queries.users.UserField
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

import java.util.concurrent.TimeUnit

@RestController
class IngestVkController {

    static String DATA_FILE_PATH = System.getenv('INGEST_VK_DATA_FILE_PATH') ?: '/data/vk.data'
    static Integer DEFAULT_SEED_ID = Integer.parseInt(System.getenv('INGEST_VK_DEFAULT_SEED_ID') ?: '3542756')

    VkApiClient vk = new VkApiClient(HttpTransportClient.getInstance())
    JsonSlurper jsonSlurper = new JsonSlurper()
    File dataFile = new File(DATA_FILE_PATH)

    @RequestMapping(path = '/ingest', method = RequestMethod.POST)
    @ResponseBody IngestResponse ingest(@RequestBody IngestRequest ingestRequest) {
        int index = 0
        long startTime = System.currentTimeMillis()
        int ingestedCount = 0

        Set<Integer> ids = readAllIds()
        if (ids.empty) {
            Integer seedId = ingestRequest.seedId ?: DEFAULT_SEED_ID
            VkProfile seed = ingestById(seedId)
            insertProfile seed
            ingestedCount++
            ids.add seedId
        }

        while (true) {
            if (index >= ids.size()) {
                break
            }

            if (ingestRequest.timeLimit != null && System.currentTimeMillis() >= startTime + TimeUnit.SECONDS.toMillis(ingestRequest.timeLimit)) {
                break
            }

            if (ingestRequest.dataSizeLimit != null && ids.size() >= ingestRequest.dataSizeLimit) {
                break
            }

            if (ingestRequest.ingestedLimit != null && ingestedCount >= ingestRequest.ingestedLimit) {
                break
            }

            List<Integer> friendsIds = getFriendsIds(ids[index])
            friendsIds.findAll({
                !ids.contains(it)
            }).each({
                ids.add it
                VkProfile newProfile = ingestById it
                insertProfile newProfile
                ingestedCount++
            })

            index++
        }

        return new IngestResponse(timeTaken: TimeUnit.SECONDS.convert(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS), recordsIngested: ingestedCount, recordsCount: ids.size())
    }

    Set<Integer> readAllIds() {
        if (!dataFile.exists()) {
            return []
        }

        List<Integer> ids = []
        dataFile.eachLine { String line ->
            ids.add jsonSlurper.parseText(line).vkId
        }

        return ids
    }

    VkProfile ingestById(Integer id) {
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        VkProfile.fromVkAPI(
                vk.users()  .get()
                            .userIds(id.toString())
                            .fields(UserField.CITY, UserField.COUNTRY, UserField.EDUCATION, UserField.UNIVERSITIES)
                            .execute().get(0)
        )
    }

    void insertProfile(VkProfile vkProfile) {
        if (!dataFile.exists()) {
            dataFile.createNewFile()
        }

        dataFile.append "${JsonOutput.toJson(vkProfile)}\n"
    }

    List<Integer> getFriendsIds(Integer id) {
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))
        
        vk.friends().get()
                    .userId(id)
                    .execute()
                    .items
    }
}
