package com.odafa.controlstation.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class VideoFeedManager implements Runnable {
	private static final int SERVER_PORT = 1313;
	private final static int UDP_MAX_PACKET_SIZE = 65507;
	private final static int DRONE_ID_LENGTH = 12;

	private DatagramSocket videoReceiverDatagramSocket;
	private ExecutorService serverRunner;
	private Map<String, Set<WebSocketSession>> droneIdToWebSocketSession;

	public VideoFeedManager() {
		try {
			videoReceiverDatagramSocket = new DatagramSocket(SERVER_PORT);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		serverRunner = Executors.newSingleThreadExecutor();
		droneIdToWebSocketSession = new ConcurrentHashMap<>();

		activate();
	}

	public void activate() {
		serverRunner.execute(this);
	}

	public void run() {
		while (!videoReceiverDatagramSocket.isClosed()) {
			try {
				byte buf[] = new byte[UDP_MAX_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);

				videoReceiverDatagramSocket.receive(packet);

				String droneId = new String(packet.getData(), 0, DRONE_ID_LENGTH);
				String data = new String(packet.getData(), DRONE_ID_LENGTH, packet.getLength());

				Set<WebSocketSession> droneIdWebSessions = droneIdToWebSocketSession.get(droneId);
				
				if (droneIdWebSessions == null) {
					continue;
				}

				Iterator<WebSocketSession> it = droneIdWebSessions.iterator();
				
				while(it.hasNext()) {
					WebSocketSession session = it.next();
					if (!session.isOpen()) {
						it.remove();
						continue;
					}
					session.sendMessage(new TextMessage(data));
				}
				
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		log.warn("Video Feed Manager Closed");
	}

	public void setVideoWebSocketSessionForDroneId(WebSocketSession newWebSocketSession, String droneId) {
		Set<WebSocketSession> droneIdSessions = droneIdToWebSocketSession.putIfAbsent(droneId, new HashSet<>());
		if(droneIdSessions == null) {
			droneIdSessions = droneIdToWebSocketSession.get(droneId);
		} 
		droneIdSessions.add(newWebSocketSession);
		log.debug("Drone ID {} has {} active Web Socket Sessions");
	}

	public boolean isServerClosed() {
		return videoReceiverDatagramSocket.isClosed();
	}

	public void shutdown() {
		if (!videoReceiverDatagramSocket.isClosed()) {
			try {
				videoReceiverDatagramSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		serverRunner.shutdown();
	}
}
