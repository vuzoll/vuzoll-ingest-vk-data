package com.github.vuzoll.ingestvk.controller

import com.github.vuzoll.ingestvk.service.ImportDataService
import com.github.vuzoll.tasks.domain.Job
import com.github.vuzoll.tasks.service.JobsService
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Slf4j
class ImportDataController {

    @Autowired
    JobsService jobsService

    @Autowired
    ImportDataService importDataService

    @PostMapping(path = '/import')
    @ResponseBody Job ingest() {
        log.info "Receive import request: $importRequest"

        return jobsService.startJob(importDataService.importDataJob())
    }
}
