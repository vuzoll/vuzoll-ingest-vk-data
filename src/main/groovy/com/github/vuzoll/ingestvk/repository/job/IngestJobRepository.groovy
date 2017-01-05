package com.github.vuzoll.ingestvk.repository.job

import com.github.vuzoll.ingestvk.domain.job.IngestJob
import org.springframework.data.repository.PagingAndSortingRepository

interface IngestJobRepository extends PagingAndSortingRepository<IngestJob, String> {

    Collection<IngestJob> findByStatus(String status)
}
