package com.example.doublesir.config;

import com.example.doublesir.service.RoomManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public RoomManager roomManager() {
        return new RoomManager();
    }
}
