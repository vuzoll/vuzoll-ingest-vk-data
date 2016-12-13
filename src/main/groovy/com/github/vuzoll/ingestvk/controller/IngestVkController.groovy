package com.github.vuzoll.ingestvk.controller

import com.github.vuzoll.ingestvk.domain.VkProfile
import com.github.vuzoll.ingestvk.service.DataStorageService
import com.github.vuzoll.ingestvk.service.VkService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

import java.util.concurrent.TimeUnit

@RestController
@Slf4j
class IngestVkController {

    static Integer DEFAULT_SEED_ID = Integer.parseInt(System.getenv('INGEST_VK_DEFAULT_SEED_ID') ?: '3542756')

    @Autowired
    VkService vkService

    @Autowired
    DataStorageService dataStorageService

    @RequestMapping(path = '/ingest', method = RequestMethod.POST)
    @ResponseBody IngestResponse ingest(@RequestBody IngestRequest ingestRequest) {
        log.info "Receive ingest request: $ingestRequest"

        long startTime = System.currentTimeMillis()
        int index = 0
        int ingestedCount = 0
        Set<Integer> ids = []

        String message = null

        try {
            log.info 'Reading already ingested data...'
            ids = dataStorageService.readAllIds()
            if (ids.empty) {
                Integer seedId = ingestRequest.seedId ?: DEFAULT_SEED_ID
                log.warn "There is no ingested data so far. Will use id:$seedId as seed profile"

                VkProfile seed = vkService.ingestVkUserById(seedId)
                dataStorageService.insertProfile(seed)
                ingestedCount++
                ids.add(seedId)
            }

            while (true) {
                log.info "Handling position $index / ${ids.size()} in ingestion queue"
                log.info "Ingestion already has taken ${TimeUnit.SECONDS.convert(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)} sec"
                log.info "Current dataset size: ${ids.size()} records"
                log.info "Already ingested: ${ingestedCount} records"

                if (index >= ids.size()) {
                    message = 'Ingestion queue is empty'
                    log.info message
                    break
                }

                if (ingestRequest.timeLimit != null && System.currentTimeMillis() >= startTime + TimeUnit.SECONDS.toMillis(ingestRequest.timeLimit)) {
                    message = 'Time limit is reached'
                    log.info message
                    break
                }

                if (ingestRequest.dataSizeLimit != null && ids.size() >= ingestRequest.dataSizeLimit) {
                    message = 'Dataset size limit is reached'
                    log.info message
                    break
                }

                if (ingestRequest.ingestedLimit != null && ingestedCount >= ingestRequest.ingestedLimit) {
                    message = 'Ingested records limit is reached'
                    log.info message
                    break
                }

                List<Integer> friendsIds = vkService.getFriendsIds(ids[index])
                friendsIds.findAll({
                    !ids.contains(it)
                }).each({
                    log.debug "Found new profile id:$it"
                    ids.add it
                    VkProfile newProfile = vkService.ingestVkUserById(it)
                    dataStorageService.insertProfile(newProfile)
                    ingestedCount++
                })

                index++
            }
        } catch (e) {
            message = "Error while ingesting data: ${e.message}"
            log.error(message, e)        
        } finally {
            return new IngestResponse(timeTaken: TimeUnit.SECONDS.convert(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS), recordsIngested: ingestedCount, recordsCount: ids.size(), message: message)
        }
    }
}
