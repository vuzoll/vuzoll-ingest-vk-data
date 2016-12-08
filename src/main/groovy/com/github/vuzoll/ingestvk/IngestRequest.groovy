package com.github.vuzoll.ingestvk

import groovy.transform.ToString

@ToString
class IngestRequest {

    Integer seedId
    Integer timeLimit
    Integer ingestedLimit
    Integer dataSizeLimit
}
