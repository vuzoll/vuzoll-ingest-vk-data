package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.ingestvk.controller.IngestRequest
import com.github.vuzoll.ingestvk.domain.job.IngestJob
import com.github.vuzoll.ingestvk.repository.job.IngestJobRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Service
@Slf4j
class IngestJobsService {

    @Autowired
    IngestVkService ingestVkService

    @Autowired
    IngestJobRepository ingestJobRepository

    @Autowired
    TaskExecutor taskExecutor

    @PostConstruct
    void markAbortedJobs() {
        log.info 'Marking all aborted jobs...'
        Collection<IngestJob> abortedJobs = ingestJobRepository.findByStatus('RUNNING')
        if (abortedJobs.empty) {
            log.info 'Found no aborted jobs'
        } else {
            log.warn "Found ${abortedJobs.size()} aborted jobs"
            abortedJobs.each { it.status = 'ABORTED' }
            ingestJobRepository.save(abortedJobs)
        }
    }

    IngestJob getCurrentlyRunningJob() {
        ingestJobRepository.findByStatus('RUNNING').first()
    }

    IngestJob startNewIngestJob(IngestRequest ingestRequest) {
        if (ingestRequest.method == 'randomized-bfs') {
            IngestJob startedJob = new IngestJob()
            startedJob.request = ingestRequest
            startedJob.status = 'RUNNING'

            startedJob = ingestJobRepository.save startedJob

            taskExecutor.execute({ ingestVkService.randomizedBfsIngest(startedJob) })

            return startedJob
        } else {
            log.error "Unknown ingest method: $ingestRequest.method"
            throw new IllegalArgumentException("Unknown ingest method: $ingestRequest.method")
        }
    }

    IngestJob jobStatus(String jobId) {
        ingestJobRepository.findOne(jobId)
    }

    IngestJob stopJob(String jobId) {
        IngestJob ingestJob = ingestJobRepository.findOne(jobId)
        ingestJob.status = 'STOPPED'
        ingestJobRepository.save ingestJob
    }

    List<IngestJob> allJobs() {
        ingestJobRepository.findAll()
    }
}
