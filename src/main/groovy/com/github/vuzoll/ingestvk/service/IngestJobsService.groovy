package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.ingestvk.controller.IngestRequest
import com.github.vuzoll.ingestvk.domain.job.IngestJob
import com.github.vuzoll.ingestvk.domain.job.JobStatus
import com.github.vuzoll.ingestvk.repository.job.IngestJobRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.TaskExecutor
import org.springframework.data.domain.Sort
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
        Collection<IngestJob> abortedJobs = ingestJobRepository.findByStatus(JobStatus.RUNNING.toString())
        if (abortedJobs.empty) {
            log.info 'Found no aborted jobs'
        } else {
            log.warn "Found ${abortedJobs.size()} aborted jobs"
            abortedJobs.each { it.status = JobStatus.ABORTED.toString() }
            ingestJobRepository.save(abortedJobs)
        }
    }

    IngestJob getCurrentlyRunningJob() {
        Collection<IngestJob> currentlyRunningJobs = ingestJobRepository.findByStatus(JobStatus.RUNNING.toString())

        if (currentlyRunningJobs.empty) {
            return null
        }

        if (currentlyRunningJobs.size() > 1) {
            log.error("There are more than one running job: ${currentlyRunningJobs}")
            throw new IllegalStateException("There are more than one running job: ${currentlyRunningJobs}")
        }

        return currentlyRunningJobs.first()
    }

    IngestJob startNewIngestJob(IngestRequest ingestRequest) {
        if (ingestRequest.method == 'randomized-bfs') {
            IngestJob startedJob = new IngestJob()
            startedJob.request = ingestRequest
            startedJob.status = JobStatus.RUNNING.toString()

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
        ingestJob.status = JobStatus.STOPPING.toString()
        ingestJobRepository.save ingestJob
    }

    List<IngestJob> allJobs() {
        ingestJobRepository.findAll(new Sort(Sort.Direction.DESC, 'startTimestamp'))
    }
}
