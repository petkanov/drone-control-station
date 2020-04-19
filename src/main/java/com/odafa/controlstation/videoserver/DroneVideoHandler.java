package com.odafa.controlstation.videoserver;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.odafa.controlstation.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DroneVideoHandler implements Runnable {
	private final String droneId;
	private final Object lock = new Object();
	
	private final Socket droneSocket;
	private final InputStream streamIn;

	private final ExecutorService controller;

	private WebSocketSession session;

	public DroneVideoHandler(Socket clientSocket, final String id) {
		this.droneId = id;
		this.droneSocket = clientSocket;
		this.controller = Executors.newFixedThreadPool(2);

		try {
			this.streamIn = clientSocket.getInputStream();
			
		} catch (IOException e) {
			close();
			throw new RuntimeException(e);
		}
	}

	public String getDroneId() {
		return this.droneId;
	}

	public void activate() {
		controller.execute(this);
	}

	public void run() {
		if (droneSocket.isClosed()) {
			return;
		}
		byte[] result = null;
		while (!droneSocket.isClosed()) {
			try {
				if(this.session != null && session.isOpen()) {
					result = Utils.readNetworkMessage(streamIn);
					this.session.sendMessage(new TextMessage(new String(result)));
				}
				if(this.session == null || !session.isOpen()) {
					synchronized(lock) {
						lock.wait();
					}
				}
			} catch(EOFException ef) {
				close();
			}
			catch (Exception e) {
				log.info("Video Feed with "+droneId+" Closed\n");
			}
		}
	}

	public boolean isClientSocketClosed() {
		return droneSocket.isClosed();
	}
	
	public void close() {
		try {
			if (!droneSocket.isClosed()) {
				droneSocket.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			controller.shutdown();
			log.info("Video Connection with Drone "+droneId+" Closed\n");
		}
	}

	public void setWebSocketSessionForVideoFeed(WebSocketSession session) {
		this.session = session;
		synchronized(lock) {
			lock.notify();
		}
		log.debug("Activating Video WebSocket Stream for Drone ID: " + droneId);
	}
}
