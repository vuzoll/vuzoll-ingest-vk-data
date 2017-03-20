package com.github.vuzoll.ingestvk.controller

import com.github.vuzoll.ingestvk.service.IngestVkService
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.github.yermilov.kerivnyk.domain.Job
import io.github.yermilov.kerivnyk.service.KerivnykService
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
    KerivnykService kerivnykService

    @Autowired
    IngestVkService ingestVkService

    @PostMapping(path = '/ingest/randomized-bfs/{datasetName}')
    @ResponseBody Job ingestUsingRandomizedBfs(@PathVariable String datasetName, @RequestParam('name') Integer seedId) {
        log.info "Receive request to start ingest using randomized bfs"

        return kerivnykService.asyncStartJob(ingestVkService.ingestUsingRandomizedBfsJob(datasetName, seedId))
    }

    @PostMapping(path = '/ingest/bfs/{datasetName}')
    @ResponseBody Job ingestUsingBfs(@PathVariable String datasetName, @RequestParam('name') Integer seedId) {
        log.info "Receive request to start ingest using bfs"

        return kerivnykService.asyncStartJob(ingestVkService.ingestUsingBfsJob(datasetName, seedId))
    }

    @PostMapping(path = '/ingest/group-bfs/{datasetName}')
    @ResponseBody Job ingestUsingGroupBfs(@PathVariable String datasetName, @RequestBody IngestUsingGroupBfsRequest request) {
        log.info "Receive request to start ingest using group bfs: ${request}"

        return kerivnykService.asyncStartJob(ingestVkService.ingestUsingGroupBfsJob(datasetName, request.seedGroupIds, request.universityIdsToAccept))
    }

    @ToString(includeNames = true)
    static class IngestUsingGroupBfsRequest {

        Collection<String> seedGroupIds
        Collection<Integer> universityIdsToAccept
    }
}
