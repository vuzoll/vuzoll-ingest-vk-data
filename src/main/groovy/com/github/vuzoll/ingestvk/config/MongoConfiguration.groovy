package com.github.vuzoll.ingestvk.config

import com.mongodb.Mongo
import org.springframework.context.annotation.Configuration
import org.springframework.data.authentication.UserCredentials
import org.springframework.data.mongodb.config.AbstractMongoConfiguration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories
class MongoConfiguration extends AbstractMongoConfiguration {

    static String DATABASE_NAME = System.getenv('INGEST_VK_MONGO_DATABASE_NAME') ?: 'vkIngested'
    static String HOST = System.getenv('INGEST_VK_MONGO_HOST') ?: 'vuzoll_mongo'
    static Integer PORT = System.getenv('INGEST_VK_MONGO_PORT') ? Integer.parseInt(System.getenv('INGEST_VK_MONGO_PORT')) : 27017
    static String USERNAME = System.getenv('INGEST_VK_MONGO_USERNAME') ?: 'vuzollAdmin'
    static String PASSWORD = System.getenv('INGEST_VK_MONGO_PASSWORD')

    @Override
    protected String getDatabaseName() {
        DATABASE_NAME
    }

    @Override
    Mongo mongo() throws Exception {
        new Mongo(HOST, PORT)
    }

    @Override
    protected UserCredentials getUserCredentials() {
        new UserCredentials(USERNAME, PASSWORD)
    }

    @Override
    protected String getMappingBasePackage() {
        'com.github.vuzoll.ingestvk.domain'
    }
}