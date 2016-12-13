package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.ingestvk.domain.VkProfile
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@Service
@Slf4j
class DataStorageService {

    static String DATA_FILE_PATH = System.getenv('INGEST_VK_DATA_FILE_PATH') ?: '/data/vk.data'

    File dataFile = new File(DATA_FILE_PATH)

    JsonSlurper jsonSlurper = new JsonSlurper()

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

    void insertProfile(VkProfile vkProfile) {
        if (!dataFile.exists()) {
            log.warn 'Data file not exists, creating it...'
            dataFile.createNewFile()
        }

        log.debug "Adding profile id:$vkProfile.vkId to the storage..."
        dataFile.append "${JsonOutput.toJson(vkProfile)}\n"
    }
}
