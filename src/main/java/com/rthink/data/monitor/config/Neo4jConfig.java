package com.rthink.data.monitor.config;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableNeo4jRepositories(basePackages = { "com.rthink.scala.test.dao" })
@EntityScan(basePackages = { "com.rthink.assets.model" })
@EnableTransactionManagement
public class Neo4jConfig {
	@Value("${rthink.neo4j.url}")
	private String url;
	@Value("${rthink.neo4j.username}")
	private String username;
	@Value("${rthink.neo4j.password}")
	private String password;

	@Bean
	public SessionFactory sessionFactory() {
		// with domain entity base package(s)
		return new SessionFactory(configuration(), "com.rthink.scala.test.model");
	}

	@Bean
	public org.neo4j.ogm.config.Configuration configuration() {
		org.neo4j.ogm.config.Configuration configuration = new org.neo4j.ogm.config.Configuration.Builder().uri(url)
				.credentials(username, password).build();
		return configuration;
	}

	@Bean
	public Neo4jTransactionManager transactionManager() {
		return new Neo4jTransactionManager(sessionFactory());
	}

//	@Bean(name = "neo4jSession")
//	public Session neo4jSession() {
//		Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password));
//		return driver.session();
//	}
}
