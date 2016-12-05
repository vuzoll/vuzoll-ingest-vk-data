package com.github.vuzoll.ingestvk

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.httpclient.HttpTransportClient
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
class IngestVkController {

    VkApiClient vk = new VkApiClient(HttpTransportClient.getInstance())

    @RequestMapping(path = '/ingest', method = RequestMethod.POST)
    @ResponseBody IngestRequest ingest(@RequestBody IngestRequest ingestRequest) {
//        def users = vk.friends().get()
//                .userId(3542756)
//                .execute()
        ingestRequest
    }
}
