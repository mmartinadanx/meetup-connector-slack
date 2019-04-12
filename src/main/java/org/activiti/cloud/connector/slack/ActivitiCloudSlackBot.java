package org.activiti.cloud.connector.slack;

import me.ramswaroop.jbot.core.slack.Bot;
import me.ramswaroop.jbot.core.slack.Controller;
import me.ramswaroop.jbot.core.slack.EventType;
import me.ramswaroop.jbot.core.slack.models.Event;
import me.ramswaroop.jbot.core.slack.models.Message;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.cloud.connector.ConnectorConstants;
import org.activiti.cloud.connector.impl.ProcessRuntimeChannels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

@Component
@EnableBinding(ProcessRuntimeChannels.class)
public class ActivitiCloudSlackBot extends Bot {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiCloudSlackBot.class);

	Map<String, WebSocketSession> channelsMap = new ConcurrentHashMap<>();

	@Value("${slackBotToken}")
	private String slackToken;
	
	@Value("${translateProcessKey}")
	private String translateProcessKey;

	public ActivitiCloudSlackBot() {
		super();
	}

	@Autowired
	private ProcessRuntimeChannels processRuntimeChannels;

	@Override
	public String getSlackToken() {
		return slackToken;
	}

	@Override
	public Bot getSlackBot() {
		return this;
	}

	public void pushMessageToChannel(String channel, String text) throws IOException {
		Message message = new Message(text);

		message.setType(EventType.MESSAGE.name().toLowerCase());
		message.setUser(slackService.getCurrentUser().getName());

		WebSocketSession webSocketSession = channelsMap.get(channel);
		if (webSocketSession != null) {
			message.setChannel(channel);
			webSocketSession.sendMessage(new TextMessage(message.toJSONString()));
		} 
	}
	
	@Controller(pattern = "^[Tt]ranslate ([\\w ]+) to (\\w+)")
	public void onReceiveRequestMessage(WebSocketSession session, Event event, Matcher matcher) {
		
		channelsMap.put(event.getChannelId(), session);
		
		final String inputText = matcher.group(1);
		final String language = matcher.group(2);
		final String channelId = event.getChannelId();
		
		LOGGER.info("Instantiating process. inputText={}, language={}, channel={}", inputText, language, channelId);
		
		StartProcessPayload startProcessInstanceCmd = ProcessPayloadBuilder.start()
				.withProcessDefinitionKey(translateProcessKey)
				.withVariable(ConnectorConstants.INPUT_TEXT, inputText)
				.withVariable(ConnectorConstants.LANGUAGE, language)
				.withVariable(ConnectorConstants.CHANNEL, channelId)
				.build();

		processRuntimeChannels.myCmdProducer().send(MessageBuilder.withPayload(startProcessInstanceCmd).build());
	}
}
