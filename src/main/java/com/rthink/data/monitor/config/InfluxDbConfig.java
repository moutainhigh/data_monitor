package com.rthink.data.monitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rthink.data.monitor.util.InfluxDbUtils;

@Configuration
public class InfluxDbConfig {
	@Value("${rthink.influxdb.url}")
    private String influxDBUrl;
    @Value("${rthink.influxdb.username}")
    private String userName;
    @Value("${rthink.influxdb.password}")
    private String password;
    @Value("${rthink.influxdb.database}")
    private String database;
    
    @Bean
    public InfluxDbUtils influxDbUtils() {
        return new InfluxDbUtils(userName, password, influxDBUrl, database, "");
    }
}
