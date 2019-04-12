package org.activiti.cloud.connector;

import org.activiti.cloud.connector.slack.ActivitiCloudSlackBot;
import org.activiti.cloud.connectors.starter.configuration.EnableActivitiCloudConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableActivitiCloudConnector
@EnableScheduling
@ComponentScan({ "me.ramswaroop.jbot", "org.activiti.cloud.connector" })
public class CloudConnectorApp {

	@Autowired
	ActivitiCloudSlackBot bot;

	public static void main(String[] args) {
		SpringApplication.run(CloudConnectorApp.class, args);
	}
}