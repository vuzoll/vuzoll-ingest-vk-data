package com.github.vuzoll.ingestvk.controller

import groovy.transform.ToString

@ToString(includeNames = true, ignoreNulls = true)
class IngestRequest {

    Integer seedId
    Integer timeLimit
    Integer ingestedLimit
    Integer dataSizeLimit
}
