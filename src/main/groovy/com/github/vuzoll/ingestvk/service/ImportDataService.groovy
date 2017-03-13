package com.github.vuzoll.ingestvk.service

import com.github.vuzoll.tasks.service.DurableJob
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@Service
@Slf4j
class ImportDataService {

    DurableJob importDataJob() {
        new DurableJob('import data') {

            @Override
            void doSomething(Closure statusUpdater) {
                //
                markFinished()
            }
        }
    }
}
