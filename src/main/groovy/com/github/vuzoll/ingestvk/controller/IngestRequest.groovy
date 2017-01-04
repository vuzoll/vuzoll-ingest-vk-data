package com.github.vuzoll.ingestvk.controller

import groovy.transform.ToString

@ToString(includeNames = true, ignoreNulls = true)
class IngestRequest {

    String method

    String timeLimit
    Integer ingestedLimit
    Integer dataSizeLimit

    Map<String, String> parameters
}
