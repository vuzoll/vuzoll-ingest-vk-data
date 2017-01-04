package com.github.vuzoll.ingestvk.repository

import com.github.vuzoll.ingestvk.domain.VkProfile
import org.springframework.data.repository.PagingAndSortingRepository

interface VkProfileRepository extends PagingAndSortingRepository<VkProfile, String> {

    long countByVkId(Integer vkId)
}