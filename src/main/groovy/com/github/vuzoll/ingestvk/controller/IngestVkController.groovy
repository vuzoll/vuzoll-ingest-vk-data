package com.github.vuzoll.ingestvk.controller

import com.github.vuzoll.ingestvk.service.IngestVkService
import com.github.vuzoll.tasks.domain.Job
import com.github.vuzoll.tasks.service.JobsService
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Slf4j
class IngestVkController {

    @Autowired
    JobsService jobsService

    @Autowired
    IngestVkService ingestVkService

    @PostMapping(path = '/ingest/randomized-bfs/{datasetName}')
    @ResponseBody Job ingestUsingRandomizedBfs(@PathVariable String datasetName, @RequestParam('name') Integer seedId) {
        log.info "Receive request to start ingest using randomized bfs"

        return jobsService.startJob(ingestVkService.ingestUsingRandomizedBfsJob(datasetName, seedId))
    }

    @PostMapping(path = '/ingest/bfs/{datasetName}')
    @ResponseBody Job ingestUsingBfs(@PathVariable String datasetName, @RequestParam('name') Integer seedId) {
        log.info "Receive request to start ingest using bfs"

        return jobsService.startJob(ingestVkService.ingestUsingBfsJob(datasetName, seedId))
    }

    @PostMapping(path = '/ingest/group-bfs/{datasetName}')
    @ResponseBody Job ingestUsingGroupBfs(@PathVariable String datasetName, @RequestBody IngestUsingGroupBfsRequest request) {
        log.info "Receive request to start ingest using group bfs"

        return jobsService.startJob(ingestVkService.ingestUsingGroupBfsJob(datasetName, request.seedGroupIds, request.universityIdsToAccept))
    }

    @ToString(includeNames = true)
    class IngestUsingGroupBfsRequest {

        Collection<String> seedGroupIds
        Collection<Integer> universityIdsToAccept
    }
}
