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
        String finalUri = mongoUri;
        // Si la URI no termina en /FurentDataBase la forzamos
        if (!finalUri.contains("/FurentDataBase")) {
            if (finalUri.contains("?")) {
                finalUri = finalUri.replace("?", "/FurentDataBase?");
            } else {
                finalUri += "/FurentDataBase";
            }
        }
        
        System.out.println(">>> [DEBUG] URI de Mongo final: " + finalUri.replaceAll(":.*@", ":****@")); // Ocultar pass en logs
        
        ConnectionString connectionString = new ConnectionString(finalUri);
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
        return new MongoTemplate(mongoClient(), "FurentDataBase");
    }
}
