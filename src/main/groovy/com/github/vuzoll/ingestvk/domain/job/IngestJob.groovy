package com.github.vuzoll.ingestvk.domain.job

import com.github.vuzoll.ingestvk.controller.IngestRequest
import org.springframework.data.annotation.Id

class IngestJob {

    @Id
    String id

    IngestRequest request

    Long startTimestamp
    String startTime

    String lastUpdateTime

    String endTime
    String timeTaken

    String status
    String message

    Integer ingestedCount
    Integer datasetSize

    List<IngestJobLog> ingestJobLogs
}
