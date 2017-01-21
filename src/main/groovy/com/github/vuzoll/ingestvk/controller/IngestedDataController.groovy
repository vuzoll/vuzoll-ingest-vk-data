package com.github.vuzoll.ingestvk.controller

import com.github.vuzoll.ingestvk.domain.vk.VkProfile
import com.github.vuzoll.ingestvk.repository.vk.VkProfileRepository
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Slf4j
class IngestedDataController {

    @Autowired
    VkProfileRepository vkProfileRepository

    @GetMapping(path = '/ingested/profile/count')
    @ResponseBody Long ingestedProfileCount() {
        log.info 'Receive ingested profiles count request'

        return vkProfileRepository.count()
    }

    @GetMapping(path = '/ingested/profile/random')
    @ResponseBody VkProfile randomIngestedProfile() {
        log.info 'Receive random ingested profile request'

        int datasetSize = vkProfileRepository.count()
        int randomVkProfileIndex = RandomUtils.nextInt(0, datasetSize)
        return vkProfileRepository.findAll(new PageRequest(randomVkProfileIndex, 1)).content.first()
    }

    @GetMapping(path = '/ingested/profile/id/{id}')
    @ResponseBody VkProfile ingestedProfileById(@PathVariable String id) {
        log.info "Receive request for ingested profile with id=${id}"

        vkProfileRepository.findOne(id)
    }

    @GetMapping(path = '/ingested/profile/vkId/{vkId}')
    @ResponseBody VkProfile ingestedProfileByVkId(@PathVariable Integer vkId) {
        log.info "Receive request for ingested profile with vkId=${vkId}"

        vkProfileRepository.findOneByVkId(vkId)
    }

    @DeleteMapping(path = '/ingested/profile')
    void deleteAllIngestedProfiles() {
        log.warn 'Receive request to delete all ingested records'

        vkProfileRepository.deleteAll()
    }
}
