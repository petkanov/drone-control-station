package com.odafa.controlstation.videoserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.odafa.controlstation.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DroneVideoServer implements Runnable {
	private static final int SERVER_PORT = 1313;
	
	private ServerSocket serverSocket;
	private ExecutorService serverRunner;
	private Map<String, DroneVideoHandler> droneIdToHandler;
	
	public DroneVideoServer() {
		try {
			serverSocket = new ServerSocket(SERVER_PORT);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		serverRunner = Executors.newSingleThreadExecutor();
		droneIdToHandler = new HashMap<>();
		
		activate();
	}

	public void activate() {
		serverRunner.execute(this);
	}

	public void run() {
		try {
			while (true) {
				if (!serverSocket.isClosed()) {
					final Socket clientSocket = serverSocket.accept();
					try {
						final String droneId = new String(Utils.readNetworkMessage(clientSocket.getInputStream()));
								
						final DroneVideoHandler droneVideo = new DroneVideoHandler(clientSocket, droneId);
						droneVideo.activate();
						
						droneIdToHandler.put(droneId, droneVideo);
						
						log.info("Video Connection Established ID: {}, IP: {}", droneId, clientSocket.getInetAddress().toString());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					break;
				}
			}
		} catch (SocketException se) {
			log.error("Video Server is shutting down y'all..");
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			shutdown();
		}
	}

	public void setVideoWebSocketSessionForDroneId(WebSocketSession session, String droneId) {
		if(droneIdToHandler.get(droneId) != null) {
			droneIdToHandler.get(droneId).setWebSocketSessionForVideoFeed(session);
		}
	}

	public boolean isServerClosed() {
		return serverSocket.isClosed();
	}

	public void shutdown() {
		if (!serverSocket.isClosed()) {
			try {
				serverSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		serverRunner.shutdown();
	}
}
