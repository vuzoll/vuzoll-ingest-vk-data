package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.ingestvk.domain.job.IngestJob
import com.github.vuzoll.ingestvk.domain.job.IngestJobLog
import com.github.vuzoll.ingestvk.domain.job.JobStatus
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
import java.util.concurrent.TimeUnit

@Service
@Slf4j
class IngestVkService {

    static final long LOG_DELTA = TimeUnit.HOURS.toMillis(1)

    static final Integer DEFAULT_SEED_ID = Integer.parseInt(System.getenv('INGEST_VK_DEFAULT_SEED_ID') ?: '3542756')

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
        try {
            ingestJob.startTimestamp = System.currentTimeMillis()
            ingestJob.startTime = LocalDateTime.now().toString()
            ingestJob.ingestedCount = 0
            ingestJobRepository.save ingestJob

            while (true) {
                ingestJob = ingestJobRepository.findOne(ingestJob.id)

                ingestJob.datasetSize = vkProfileRepository.count() as Integer
                ingestJob.lastUpdateTime = LocalDateTime.now().toString()
                ingestJob.timeTaken = toDurationString(System.currentTimeMillis() - ingestJob.startTimestamp)

                if (ingestJob.ingestJobLogs.empty || System.currentTimeMillis() - ingestJob.ingestJobLogs.timestamp.max() > LOG_DELTA) {
                    IngestJobLog ingestJobLog = new IngestJobLog()
                    ingestJobLog.timestamp = System.currentTimeMillis()
                    ingestJobLog.time = LocalDateTime.now().toString()
                    ingestJobLog.timeTaken = ingestJob.timeTaken
                    ingestJobLog.status = ingestJob.status
                    ingestJobLog.datasetSize = ingestJob.datasetSize
                    ingestJobLog.ingestedCount = ingestJob.ingestedCount
                    ingestJob.ingestJobLogs = ingestJob.ingestJobLogs.empty ? [ ingestJobLog ] : ingestJob.ingestJobLogs + ingestJobLog
                }

                ingestJobRepository.save ingestJob

                log.info "JobId=${ingestJob.id}: ingestion already has taken ${toDurationString(System.currentTimeMillis() - ingestJob.startTimestamp)}"
                log.info "JobId=${ingestJob.id}: current dataset size is ${ingestJob.datasetSize} records"
                log.info "JobId=${ingestJob.id}: already ingested ${ingestJob.ingestedCount} records"

                if (ingestJob.request.timeLimit != null && System.currentTimeMillis() >= ingestJob.startTimestamp + fromDurationString(ingestJob.request.timeLimit)) {
                    ingestJob.message = 'time limit is reached'
                    log.info "JobId=${ingestJob.id}: ${ingestJob.message}"
                    break
                }

                if (ingestJob.request.dataSizeLimit != null && ingestJob.datasetSize >= ingestJob.request.dataSizeLimit) {
                    ingestJob.message = 'dataset size limit is reached'
                    log.info "JobId=${ingestJob.id}: ${ingestJob.message}"
                    break
                }

                if (ingestJob.request.ingestedLimit != null && ingestJob.ingestedCount >= ingestJob.request.ingestedLimit) {
                    ingestJob.message = 'ingested records limit is reached'
                    log.info "JobId=${ingestJob.id}: ${ingestJob.message}"
                    break
                }

                if (ingestJob.status == JobStatus.STOPPING.toString()) {
                    ingestJob.message = 'stopped by client request'
                    log.info "JobId=${ingestJob.id}: ${ingestJob.message}"
                    break
                }

                if (ingestJob.datasetSize == 0) {
                    Integer seedId = ingestJob.request.parameters?.getOrDefault('seedId', DEFAULT_SEED_ID) ?: DEFAULT_SEED_ID
                    log.warn "JobId=${ingestJob.id}: dataset is empty - using seed profile with id=$seedId to initialize it..."

                    VkProfile seedProfile = vkService.ingestVkProfileById(seedId)
                    vkProfileRepository.save seedProfile
                    continue
                }

                int randomVkProfileIndex = RandomUtils.nextInt(0, ingestJob.datasetSize)
                VkProfile randomVkProfile = vkProfileRepository.findAll(new PageRequest(randomVkProfileIndex, 1)).content.first()

                log.info "JobId=${ingestJob.id}: using profile with id=$randomVkProfile.vkId for the next ingestion iteration..."
                randomVkProfile.friendsIds.findAll({ Integer friendVkId ->
                    vkProfileRepository.countByVkId(friendVkId) == 0
                }).each({ Integer friendVkId ->
                    log.info "JobId=${ingestJob.id}: found new profile with id=$friendVkId"
                    ingestJob.ingestedCount++

                    VkProfile newProfile = vkService.ingestVkProfileById(friendVkId)
                    vkProfileRepository.save newProfile
                })
            }

            ingestJob.endTime = LocalDateTime.now().toString()
            ingestJob.lastUpdateTime = ingestJob.endTime
            ingestJob.timeTaken = toDurationString(System.currentTimeMillis() - ingestJob.startTimestamp)
            ingestJob.status = JobStatus.COMPLETED.toString()
            ingestJobRepository.save ingestJob
        } catch (e) {
            log.error("JobId=${ingestJob.id}: ingestion failed", e)

            ingestJob.message = "Failed because of ${e.class.name}, with message: ${e.message}"
            ingestJob.endTime = LocalDateTime.now().toString()
            ingestJob.lastUpdateTime = ingestJob.endTime
            ingestJob.timeTaken = toDurationString(System.currentTimeMillis() - ingestJob.startTimestamp)
            ingestJob.status = JobStatus.FAILED.toString()
            ingestJobRepository.save ingestJob

            throw e
        }
    }

    static long fromDurationString(String durationAsString) {
        TIME_LIMIT_FORMAT.parsePeriod(durationAsString).toStandardDuration().getStandardSeconds() * 1000
    }

    static String toDurationString(long duration) {
        if (duration == 0) {
            return "0sec"
        } else {
            return TIME_LIMIT_FORMAT.print(new Period(duration))
        }
    }
}
