package com.github.vuzoll.ingestvk.controller

import com.github.vuzoll.ingestvk.domain.job.IngestJob
import com.github.vuzoll.ingestvk.service.IngestJobsService
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Slf4j
@TypeChecked
class IngestVkController {

    @Autowired
    IngestJobsService ingestJobsService

    @PostMapping(path = '/ingest')
    @ResponseBody IngestJob ingest(@RequestBody IngestRequest ingestRequest) {
        log.info "Receive ingest request: $ingestRequest"

        IngestJob currentlyRunningJob = ingestJobsService.getCurrentlyRunningJob()
        if (currentlyRunningJob != null) {
            log.error "Service is busy with another job id=$currentlyRunningJob.id, can't accept new one"
            throw new IllegalStateException("Service is busy with another job id=$currentlyRunningJob.id, can't accept new one")
        }

        return ingestJobsService.startNewIngestJob(ingestRequest)
    }

    @GetMapping(path = '/ingest/{jobId}')
    @ResponseBody IngestJob jobStatus(@PathVariable String jobId) {
        IngestJob ingestJob = ingestJobsService.jobStatus(jobId)
        if (ingestJob == null) {
            return null
        }

        if (ingestJob.ingestJobLogs != null) {
            ingestJob.ingestJobLogs = ingestJob.ingestJobLogs.sort({ -it.timestamp }).take(20)
        } else {
            ingestJob.ingestJobLogs = []
        }

        return ingestJob
    }

    @GetMapping(path = '/ingest/all')
    @ResponseBody List<IngestJob> allJobsStatus() {
        List<IngestJob> allJobs = ingestJobsService.allJobs()

        allJobs.each { IngestJob ingestJob ->
            if (ingestJob.ingestJobLogs != null) {
                ingestJob.ingestJobLogs = ingestJob.ingestJobLogs.sort({ -it.timestamp })
            } else {
                ingestJob.ingestJobLogs = []
            }
        }

        return allJobs
    }

    @GetMapping(path = '/ingest/current')
    @ResponseBody IngestJob currentJobStatus() {
        IngestJob ingestJob = ingestJobsService.getCurrentlyRunningJob()
        if (ingestJob == null) {
            return null
        }

        if (ingestJob.ingestJobLogs != null) {
            ingestJob.ingestJobLogs = ingestJob.ingestJobLogs.sort({ -it.timestamp }).take(20)
        } else {
            ingestJob.ingestJobLogs = []
        }

        return ingestJob
    }

    @GetMapping(path = '/ingest/last')
    @ResponseBody IngestJob lastJobStatus() {
        IngestJob ingestJob = ingestJobsService.getLastJob()
        if (ingestJob == null) {
            return null
        }

        if (ingestJob.ingestJobLogs != null) {
            ingestJob.ingestJobLogs = ingestJob.ingestJobLogs.sort({ -it.timestamp }).take(20)
        } else {
            ingestJob.ingestJobLogs = []
        }

        return ingestJob
    }

    @DeleteMapping(path = '/ingest/{jobId}')
    @ResponseBody IngestJob stopJob(@PathVariable String jobId) {
        IngestJob ingestJob = ingestJobsService.stopJob(jobId)
        if (ingestJob == null) {
            return null
        }

        if (ingestJob.ingestJobLogs != null) {
            ingestJob.ingestJobLogs = ingestJob.ingestJobLogs.sort({ -it.timestamp }).take(20)
        } else {
            ingestJob.ingestJobLogs = []
        }

        return ingestJob
    }
}
