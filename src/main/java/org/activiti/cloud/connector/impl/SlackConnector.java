package org.activiti.cloud.connector.impl;

import java.util.Collections;
import java.util.Map;

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.connector.ConnectorConstants;
import org.activiti.cloud.connector.slack.ActivitiCloudSlackBot;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;


@Component
@EnableBinding(SlackConnectorChannels.class)
public class SlackConnector {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SlackConnector.class);

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private ActivitiCloudSlackBot bot;

    @Autowired
    private ConnectorProperties connectorProperties;
    
    private final IntegrationResultSender integrationResultSender;

    public SlackConnector(IntegrationResultSender integrationResultSender) {

        this.integrationResultSender = integrationResultSender;
    }

    @StreamListener(value = SlackConnectorChannels.SLACK_CONNECTOR_CONSUMER)
    public void notifySlackChannel(IntegrationRequest event) throws Exception {

        LOGGER.info("Received event. process={}", event.getIntegrationContext().getProcessDefinitionId());

		//Read input variables
        final Map<String, Object> inboundVariables = event.getIntegrationContext().getInBoundVariables();      
        final String textToSend = (String)inboundVariables.get(ConnectorConstants.OUTPUT_TEXT);
        final String channel = (String)inboundVariables.get(ConnectorConstants.CHANNEL);
            
        LOGGER.info("Received message. textToSend={}, channel={}", textToSend, channel);
        
        //Execute business logic
        bot.pushMessageToChannel(channel, textToSend);

        LOGGER.info("Message sent");

        //Create response
        Message<IntegrationResult> message = IntegrationResultBuilder.resultFor(event, connectorProperties)
                .withOutboundVariables(Collections.emptyMap())
                .buildMessage();

        //Send response to Activiti
        integrationResultSender.send(message);
    }
}
