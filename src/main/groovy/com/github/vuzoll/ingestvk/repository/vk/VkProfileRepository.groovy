package com.github.vuzoll.ingestvk.repository.vk

import com.github.vuzoll.ingestvk.domain.vk.VkProfile
import org.springframework.data.repository.PagingAndSortingRepository

interface VkProfileRepository extends PagingAndSortingRepository<VkProfile, String> {

    VkProfile findOneByVkId(Integer vkId)

    VkProfile findOneByIngestionIndex(Integer ingestionIndex)
}