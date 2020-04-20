package com.odafa.controlstation.configuration;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.odafa.controlstation.service.VideoFeedManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketHandler extends AbstractWebSocketHandler {
	
	@Autowired
	private VideoFeedManager videoFeedManager;
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		log.debug("Connection with WebSocketHandler Established.");
	}
	
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage droneId) throws IOException {
    	videoFeedManager.setVideoWebSocketSessionForDroneId(session, droneId.getPayload());
    } 
}
