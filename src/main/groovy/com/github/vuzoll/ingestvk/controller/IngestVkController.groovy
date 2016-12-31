package com.github.vuzoll.ingestvk.controller

import com.github.vuzoll.ingestvk.domain.VkProfile
import com.github.vuzoll.ingestvk.service.DataStorageService
import com.github.vuzoll.ingestvk.service.VkService
import groovy.util.logging.Slf4j
import org.joda.time.Period
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Slf4j
class IngestVkController {

    static Integer DEFAULT_SEED_ID = Integer.parseInt(System.getenv('INGEST_VK_DEFAULT_SEED_ID') ?: '3542756')

    static final PeriodFormatter TIME_LIMIT_FORMAT = new PeriodFormatterBuilder()
            .appendHours().appendSuffix('h')
            .appendMinutes().appendSuffix('min')
            .appendSeconds().appendSuffix('sec')
            .toFormatter()

    @Autowired
    VkService vkService

    @Autowired
    DataStorageService dataStorageService

    @RequestMapping(path = '/ingest/bfs', method = RequestMethod.POST)
    @ResponseBody IngestResponse ingestBfs(@RequestBody IngestRequest ingestRequest) {
        log.info "Receive ingest request to use BFS: $ingestRequest"

        long startTime = System.currentTimeMillis()
        int index = 0
        int ingestedCount = 0
        Set<Integer> ids = []

        String message

        log.info 'Reading already ingested data...'
        ids = dataStorageService.readAllIds()
        if (ids.empty) {
            Integer seedId = ingestRequest.seedId ?: DEFAULT_SEED_ID
            log.warn "There is no ingested data so far. Will use id:$seedId as seed profile"

            VkProfile seed = vkService.ingestVkProfileById(seedId)
            dataStorageService.insertProfile(seed)
            ingestedCount++
            ids.add(seedId)
        }

        while (true) {
            log.info "Handling position $index / ${ids.size()} in ingestion queue"
            log.info "Ingestion already has taken ${toDurationString(System.currentTimeMillis() - startTime)}"
            log.info "Current dataset size: ${ids.size()} records"
            log.info "Already ingested: ${ingestedCount} records"

            if (index >= ids.size()) {
                message = 'Ingestion queue is empty'
                log.info message
                break
            }

            if (ingestRequest.timeLimit != null && System.currentTimeMillis() >= startTime + fromDurationString(ingestRequest.timeLimit)) {
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

            try {
                List<Integer> friendsIds = vkService.getFriendsIds(ids[index])
                friendsIds.findAll({
                    !ids.contains(it)
                }).each({
                    log.debug "Found new profile id:$it"
                    ids.add it
                    VkProfile newProfile = vkService.ingestVkProfileById(it)
                    dataStorageService.insertProfile(newProfile)
                    ingestedCount++
                })

                index++
            } catch (e) {
                message = "Error while ingesting data: ${e.message}"
                log.error(message, e)
            }
        }

        return new IngestResponse(timeTaken: toDurationString(System.currentTimeMillis() - startTime), recordsIngested: ingestedCount, recordsCount: ids.size(), message: message)
    }

    static long fromDurationString(String durationAsString) {
        TIME_LIMIT_FORMAT.parsePeriod(durationAsString).toStandardDuration().getStandardSeconds() * 1000
    }

    static String toDurationString(long duration) {
        TIME_LIMIT_FORMAT.print(new Period(duration))
    }
}
