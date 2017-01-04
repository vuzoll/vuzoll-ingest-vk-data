package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.ingestvk.controller.IngestRequest
import com.github.vuzoll.ingestvk.controller.IngestResponse
import com.github.vuzoll.ingestvk.domain.VkProfile
import com.github.vuzoll.ingestvk.repository.VkProfileRepository
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomUtils
import org.joda.time.Period
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
@Slf4j
class IngestVkService {

    static Integer DEFAULT_SEED_ID = Integer.parseInt(System.getenv('INGEST_VK_DEFAULT_SEED_ID') ?: '3542756')

    static final PeriodFormatter TIME_LIMIT_FORMAT = new PeriodFormatterBuilder()
            .appendHours().appendSuffix('h')
            .appendMinutes().appendSuffix('min')
            .appendSeconds().appendSuffix('sec')
            .toFormatter()

    @Autowired
    VkProfileRepository vkProfileRepository

    @Autowired
    VkService vkService

    IngestResponse randomizedBfsIngest(IngestRequest ingestRequest) {
        long startTime = System.currentTimeMillis()
        int ingestedCount = 0

        String message

        while (true) {
            long datasetSize = vkProfileRepository.count()

            log.info "Ingestion already has taken ${toDurationString(System.currentTimeMillis() - startTime)}"
            log.info "Current dataset size: ${datasetSize} records"
            log.info "Already ingested: ${ingestedCount} records"

            if (ingestRequest.timeLimit != null && System.currentTimeMillis() >= startTime + fromDurationString(ingestRequest.timeLimit)) {
                message = 'Time limit is reached'
                log.info message
                break
            }

            if (ingestRequest.dataSizeLimit != null && datasetSize >= ingestRequest.dataSizeLimit) {
                message = 'Dataset size limit is reached'
                log.info message
                break
            }

            if (ingestRequest.ingestedLimit != null && ingestedCount >= ingestRequest.ingestedLimit) {
                message = 'Ingested records limit is reached'
                log.info message
                break
            }

            if (datasetSize == 0) {
                Integer seedId = ingestRequest.parameters?.getOrDefault('seedId', DEFAULT_SEED_ID) ?: DEFAULT_SEED_ID
                log.warn "Dataset is empty. Using seed profile with id=$seedId to initialize it..."

                VkProfile seedProfile = vkService.ingestVkProfileById(seedId)
                vkProfileRepository.save seedProfile
                continue
            }

            int randomVkProfileIndex = RandomUtils.nextInt(0, datasetSize)
            VkProfile randomVkProfile = vkProfileRepository.findAll(new PageRequest(randomVkProfileIndex, 1)).content.first()

            log.info "Using profile with id=$randomVkProfile.vkId for the next ingestion iteration..."
            randomVkProfile.friendsIds.findAll({ Integer friendVkId ->
                vkProfileRepository.countByVkId(friendVkId) == 0
            }).each({ Integer friendVkId ->
                log.info "Found new profile with id=$friendVkId"

                VkProfile newProfile = vkService.ingestVkProfileById(friendVkId)
                vkProfileRepository.save newProfile
            })
        }

        return new IngestResponse(timeTaken: toDurationString(System.currentTimeMillis() - startTime), recordsIngested: ingestedCount, recordsCount: vkProfileRepository.count(), message: message)
    }

    static long fromDurationString(String durationAsString) {
        TIME_LIMIT_FORMAT.parsePeriod(durationAsString).toStandardDuration().getStandardSeconds() * 1000
    }

    static String toDurationString(long duration) {
        TIME_LIMIT_FORMAT.print(new Period(duration))
    }
}
