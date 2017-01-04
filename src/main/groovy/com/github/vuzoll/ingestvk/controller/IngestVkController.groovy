package com.github.vuzoll.ingestvk.controller

import com.github.vuzoll.ingestvk.service.DataStorageService
import com.github.vuzoll.ingestvk.service.IngestVkService
import com.github.vuzoll.ingestvk.service.VkService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Slf4j
class IngestVkController {

    @Autowired
    IngestVkService ingestVkService

    @RequestMapping(path = '/ingest', method = RequestMethod.POST)
    @ResponseBody IngestResponse ingest(@RequestBody IngestRequest ingestRequest) {
        log.info "Receive ingest request: $ingestRequest"

        if (ingestRequest.method == 'randomized-bfs') {
            return ingestVkService.randomizedBfsIngest(ingestRequest)
        } else {
            throw new IllegalArgumentException("Unknown ingest method: $ingestRequest.method")
        }
    }
}
