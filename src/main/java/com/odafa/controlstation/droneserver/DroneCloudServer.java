package com.odafa.controlstation.droneserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

import com.odafa.controlstation.dto.DataPoint;
import com.odafa.controlstation.utils.Utils;

import lombok.extern.slf4j.Slf4j;
 
@Slf4j
@Component
public class DroneCloudServer implements Runnable{
	private static final int SERVER_PORT = 1314;
	
	private final ServerSocket serverSocket;
	private final ExecutorService serverRunner;
	
	private final Map<String, DroneHandler> droneIdToHandler;
	
	public DroneCloudServer() {
		try {
			serverSocket = new ServerSocket(SERVER_PORT);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		serverRunner = Executors.newSingleThreadExecutor();
		droneIdToHandler = new ConcurrentHashMap<>();
		
		activate();
	} 
	
	public void run() {
		try {
			while (true) {
				if (!serverSocket.isClosed()) {
					final Socket clientSocket = serverSocket.accept();
					try {
						final String droneId = new String(Utils.readNetworkMessage(clientSocket.getInputStream()));
						
						final DroneHandler handler = new DroneHandler(droneId, clientSocket);
						handler.activate();
						
						droneIdToHandler.put(droneId, handler);
						
						log.info("Control Connection Established with " + clientSocket.getInetAddress().toString());
						
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				} else {
					break;
				}
			}
		} catch (SocketException se) {
			log.info("Control Server is shutting down y'all..");
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			shutdown();
		}
	}

	public void activate() {
		serverRunner.execute(this);
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
	
	public void sendMessageFromUserIdToDrone(String droneId, int commandCode) {
		final DroneHandler handler = droneIdToHandler.get(droneId);
		if(handler != null) {
			handler.sendCommand(commandCode);
		}
	}
	
	public void sendMissionDataToDrone(String droneId, List<DataPoint> dataPoints) {
		final DroneHandler handler = droneIdToHandler.get(droneId);
		if(handler != null) {
			handler.sendMissionData(dataPoints);
		}
	}
	
	public Collection<DroneHandler> getDronesHandlers(){
		return droneIdToHandler.values();
	}
}