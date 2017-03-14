package com.github.vuzoll.ingestvk.repository

import com.github.vuzoll.ingestvk.domain.VkProfile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository

interface VkProfileRepository extends PagingAndSortingRepository<VkProfile, String> {

    VkProfile findOneByDatasetNameAndVkId(String datasetName, Integer vkId)

    Integer countByDatasetName(String datasetName)

    Page<VkProfile> findByDatasetName(String datasetName, Pageable pageable)

    VkProfile findOneByDatasetNameAndIngestionIndex(String datasetName, Integer ingestionIndex)
}