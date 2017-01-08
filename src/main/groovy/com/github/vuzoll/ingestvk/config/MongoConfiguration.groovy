package com.github.vuzoll.ingestvk.config

import com.mongodb.Mongo
import com.mongodb.MongoClient
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import groovy.transform.TypeChecked
import org.springframework.context.annotation.Configuration
import org.springframework.data.authentication.UserCredentials
import org.springframework.data.mongodb.config.AbstractMongoConfiguration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories('com.github.vuzoll.ingestvk.repository')
@TypeChecked
class MongoConfiguration extends AbstractMongoConfiguration {

    static String DATABASE_NAME = System.getenv('INGEST_VK_MONGO_DATABASE_NAME') ?: 'vkIngested'
    static String AUTHENTICATION_DATABASE_NAME = System.getenv('INGEST_VK_MONGO_AUTH_DATABASE_NAME') ?: 'admin'
    static String HOST = System.getenv('INGEST_VK_MONGO_HOST') ?: 'vuzoll_mongo'
    static Integer PORT = System.getenv('INGEST_VK_MONGO_PORT') ? Integer.parseInt(System.getenv('INGEST_VK_MONGO_PORT')) : 27017
    static String USERNAME = System.getenv('INGEST_VK_MONGO_USERNAME') ?: 'ingestVkService'
    static String PASSWORD = System.getenv('INGEST_VK_MONGO_PASSWORD')

    @Override
    protected String getDatabaseName() {
        DATABASE_NAME
    }

    @Override
    Mongo mongo() throws Exception {
        new MongoClient(new ServerAddress(HOST, PORT), [ MongoCredential.createCredential(USERNAME, AUTHENTICATION_DATABASE_NAME, PASSWORD.toCharArray()) ])
    }

    @Override
    protected String getMappingBasePackage() {
        'com.github.vuzoll.ingestvk.domain'
    }
}