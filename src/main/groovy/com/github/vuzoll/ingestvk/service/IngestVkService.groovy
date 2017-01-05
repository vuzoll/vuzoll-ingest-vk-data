package com.github.vuzoll.ingestvk.service


import com.github.vuzoll.ingestvk.domain.job.IngestJob
import com.github.vuzoll.ingestvk.domain.vk.VkProfile
import com.github.vuzoll.ingestvk.repository.job.IngestJobRepository
import com.github.vuzoll.ingestvk.repository.vk.VkProfileRepository
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomUtils
import org.joda.time.Period
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

import java.time.LocalDateTime

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
    IngestJobRepository ingestJobRepository

    @Autowired
    VkApiService vkService

    void randomizedBfsIngest(IngestJob ingestJob) {
        ingestJob.startTimestamp = System.currentTimeMillis()
        ingestJob.startTime = LocalDateTime.now().toString()
        ingestJob.ingestedCount = 0
        ingestJobRepository.save ingestJob

        while (true) {
            ingestJob = ingestJobRepository.findOne(ingestJob.id)
            ingestJob.datasetSize = vkProfileRepository.count() as Integer
            ingestJobRepository.save ingestJob

            log.info "Ingestion already has taken ${toDurationString(System.currentTimeMillis() - ingestJob.startTimestamp)}"
            log.info "Current dataset size: ${ingestJob.datasetSize} records"
            log.info "Already ingested: ${ingestJob.ingestedCount} records"

            if (ingestJob.request.timeLimit != null && System.currentTimeMillis() >= ingestJob.startTimestamp + fromDurationString(ingestJob.request.timeLimit)) {
                ingestJob.message = 'Time limit is reached'
                log.info ingestJob.message
                break
            }

            if (ingestJob.request.dataSizeLimit != null && ingestJob.datasetSize >= ingestJob.request.dataSizeLimit) {
                ingestJob.message = 'Dataset size limit is reached'
                log.info ingestJob.message
                break
            }

            if (ingestJob.request.ingestedLimit != null && ingestJob.ingestedCount >= ingestJob.request.ingestedLimit) {
                ingestJob.message = 'Ingested records limit is reached'
                log.info ingestJob.message
                break
            }

            if (ingestJob.status == 'STOPPED') {
                ingestJob.message = 'Stopped by client request'
                log.info ingestJob.message
                break
            }

            if (ingestJob.datasetSize == 0) {
                Integer seedId = ingestJob.request.parameters?.getOrDefault('seedId', DEFAULT_SEED_ID) ?: DEFAULT_SEED_ID
                log.warn "Dataset is empty. Using seed profile with id=$seedId to initialize it..."

                VkProfile seedProfile = vkService.ingestVkProfileById(seedId)
                vkProfileRepository.save seedProfile
                continue
            }

            int randomVkProfileIndex = RandomUtils.nextInt(0, ingestJob.datasetSize)
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

        ingestJob.endTime = LocalDateTime.now().toString()
        ingestJob.timeTaken = toDurationString(System.currentTimeMillis() - ingestJob.startTimestamp)
        if (ingestJob.status == 'RUNNING') {
            ingestJob.status = 'COMPLETED'
        }
        ingestJobRepository.save ingestJob
    }

    static long fromDurationString(String durationAsString) {
        TIME_LIMIT_FORMAT.parsePeriod(durationAsString).toStandardDuration().getStandardSeconds() * 1000
    }

    static String toDurationString(long duration) {
        TIME_LIMIT_FORMAT.print(new Period(duration))
    }
}
