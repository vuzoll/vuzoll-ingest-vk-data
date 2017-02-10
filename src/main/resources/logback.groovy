import ch.qos.logback.classic.Level
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy

appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = '%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n'
    }
}

appender('FILE', RollingFileAppender) {
    file = '/logs/vuzoll-ingest-vk.log'

    encoder(PatternLayoutEncoder) {
        pattern = '%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n'
    }

    rollingPolicy(TimeBasedRollingPolicy) {
        FileNamePattern = "/logs/vuzoll-ingest-vk.%d{yyyy-MM-dd}.log"
    }
}

String VUZOLL_LOG_LEVEL = System.getenv('INGEST_VK_VUZOLL_LOG_LEVEL') ?: 'INFO'
String ROOT_LOG_LEVEL = System.getenv('INGEST_VK_ROOT_LOG_LEVEL') ?: 'INFO'

root(Level.toLevel(ROOT_LOG_LEVEL), ['STDOUT', 'FILE' ])
logger('com.github.vuzoll', Level.toLevel(VUZOLL_LOG_LEVEL), [ 'STDOUT', 'FILE' ], false)
