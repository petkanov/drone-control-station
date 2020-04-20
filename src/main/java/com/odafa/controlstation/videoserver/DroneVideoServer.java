package com.odafa.controlstation.videoserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.odafa.controlstation.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DroneVideoServer implements Runnable {
	private static final int SERVER_PORT = 1313;
    private final static int UDP_MAX_PACKET_SIZE = 65507;
    private final static int DRONE_ID_LENGTH = 12;

	
	private DatagramSocket rxsocket;
	private ExecutorService serverRunner;
	private Map<String, DroneVideoHandler> droneIdToHandler;
	private Map<String, WebSocketSession> droneIdToWebSocketSession;
	
	public DroneVideoServer() {
		try {
	        rxsocket = new DatagramSocket(SERVER_PORT);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		serverRunner = Executors.newSingleThreadExecutor();
		droneIdToHandler = new HashMap<>();
		droneIdToWebSocketSession = new HashMap<>();
		
		activate();
	}

	public void activate() {
		serverRunner.execute(this);
	}

	public void run() {
		while (true) {
			try {
				if (!rxsocket.isClosed()) {
					byte buf[] = new byte[UDP_MAX_PACKET_SIZE];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					rxsocket.receive(packet);
					String droneId = new String(packet.getData(), 0, DRONE_ID_LENGTH);
					String data = new String(packet.getData(), DRONE_ID_LENGTH, packet.getLength());

					System.out.println(droneId + " Received length: " + data.length());

					WebSocketSession droneWebSession = droneIdToWebSocketSession.get(droneId);
					if (droneWebSession != null && droneWebSession.isOpen()) {
						droneIdToWebSocketSession.get(droneId).sendMessage(new TextMessage(data));
					}

					/*
					 * final Socket clientSocket = serverSocket.accept(); try { final String droneId
					 * = new String(Utils.readNetworkMessage(clientSocket.getInputStream()));
					 * 
					 * final DroneVideoHandler droneVideo = new DroneVideoHandler(clientSocket,
					 * droneId); droneVideo.activate();
					 * 
					 * droneIdToHandler.put(droneId, droneVideo);
					 * 
					 * log.info("Video Connection Established ID: {}, IP: {}", droneId,
					 * clientSocket.getInetAddress().toString()); } catch (Exception e) {
					 * e.printStackTrace(); }
					 */
				} else {
					log.error("Datagram Socket Closed");
					break;
				}
			} catch (SocketTimeoutException se) {
				log.error(se.getMessage() + " Video Server is shutting down y'all..");
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
	}

	public void setVideoWebSocketSessionForDroneId(WebSocketSession session, String droneId) {
		droneIdToWebSocketSession.put(droneId, session);
	}

	public boolean isServerClosed() {
		return rxsocket.isClosed();
	}

	public void shutdown() {
		if (!rxsocket.isClosed()) {
			try {
				rxsocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		serverRunner.shutdown();
	}
}
