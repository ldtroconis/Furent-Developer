package com.alquiler.furent.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableMongoRepositories(basePackages = "com.alquiler.furent.repository")
public class MongoConfiguration {

    @Value("${spring.mongodb.uri}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .applyToSocketSettings(builder -> 
                builder.connectTimeout(30, TimeUnit.SECONDS)
                       .readTimeout(30, TimeUnit.SECONDS))
            .applyToClusterSettings(builder -> 
                builder.serverSelectionTimeout(30, TimeUnit.SECONDS))
            .build();
        
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        String databaseName = connectionString.getDatabase();
        
        if (databaseName == null || databaseName.trim().isEmpty()) {
            databaseName = "FurentDataBase";
        }
        
        System.out.println(">>> [MONGODB] Conectando a la base de datos: " + databaseName);
        return new MongoTemplate(mongoClient(), databaseName);
    }
}
