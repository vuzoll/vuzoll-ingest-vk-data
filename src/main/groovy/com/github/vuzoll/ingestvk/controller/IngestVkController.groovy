package com.github.vuzoll.ingestvk.controller

import com.github.vuzoll.ingestvk.service.IngestVkService
import com.github.vuzoll.tasks.domain.Job
import com.github.vuzoll.tasks.service.JobsService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Slf4j
class IngestVkController {

    @Autowired
    JobsService jobsService

    @Autowired
    IngestVkService ingestVkService

    @PostMapping(path = '/ingest/randomized-bfs')
    @ResponseBody Job ingestUsingRandomizedBfs() {
        log.info "Receive request to start ingest using randomized bfs"

        return jobsService.startJob(ingestVkService.ingestUsingRandomizedBfsJob())
    }

    @PostMapping(path = '/ingest/bfs')
    @ResponseBody Job ingestUsingBfs() {
        log.info "Receive request to start ingest using bfs"

        return jobsService.startJob(ingestVkService.ingestUsingBfsJob())
    }

    @PostMapping(path = '/ingest/group-bfs')
    @ResponseBody Job ingestUsingGroupBfs() {
        log.info "Receive request to start ingest using group bfs"

        return jobsService.startJob(ingestVkService.ingestUsingGroupBfsJob())
    }
}
