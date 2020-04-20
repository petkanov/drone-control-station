package com.odafa.controlstation.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.odafa.controlstation.dto.DataPoint;
import com.odafa.controlstation.dto.DroneInfoDTO;
import com.odafa.controlstation.utils.CommandBuilder;
import com.odafa.controlstation.utils.ProtoData;
import com.odafa.controlstation.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ControlHandler implements Runnable {
	private final String droneId;
	private DroneInfoDTO lastStatus;
	
	private final Socket droneSocket;
	
	private final InputStream streamIn;
	private final OutputStream streamOut;

	private final BlockingQueue<byte[]> indoxMessageBuffer;
	private final ExecutorService controller;
	
	public ControlHandler(String droneId, Socket clientSocket) {
		this.droneSocket = clientSocket;
		this.droneId = droneId;
		this.indoxMessageBuffer = new ArrayBlockingQueue<>(1024);
		this.controller = Executors.newFixedThreadPool(2);

		try {
			this.streamIn = clientSocket.getInputStream();
			this.streamOut = clientSocket.getOutputStream();
			
		} catch (IOException e) {
			close();
			throw new RuntimeException(e);
		}
	}

	public void sendCommand(int commandCode) {
		final byte[] command = Utils.createNetworkMessage( CommandBuilder.translateCommand(commandCode));
		this.indoxMessageBuffer.add(command);
		log.debug("Sending Code: " + commandCode);
	}

	public void sendMissionData(List<DataPoint> dataPoints) {
		final byte[] data = Utils.createNetworkMessage( CommandBuilder.translateMissionData(dataPoints));
		this.indoxMessageBuffer.add(data);
		log.debug("Sending Mission Data: " + dataPoints);
	}

	public void activate() {
		controller.execute(this);

		controller.execute(new Runnable() {
			public void run() {
				while (!droneSocket.isClosed()) {
					try {
						sendToDrone( indoxMessageBuffer.take());
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		});
	}
	
	private void sendToDrone(byte[] data) {
		if (!droneSocket.isClosed()) {
			if (streamOut == null) {
				return;
			}
			try {
				streamOut.write(data);
				streamOut.flush();
			} catch (SocketException se) {
				log.info("Socket has been closed: " + se.getMessage());
				close();
			} catch (IOException e) {
				log.error(e.getMessage());
			}
		}
	}

	public void run() {
		if (droneSocket.isClosed()) {
			return;
		}
		while (!droneSocket.isClosed()) {
			updateDroneInfo();
		}
		close();
	}

	public boolean isClientSocketClosed() {
		return droneSocket.isClosed();
	}

	private void updateDroneInfo() {
		try {
			
			byte[] result = Utils.readNetworkMessage(streamIn);
			
			final ProtoData.DroneData droneData = ProtoData.DroneData.parseFrom(result);
			
			final float speedInKmH = droneData.getSpeed() * 3.6f;
			
			final String webSocketURL = "/endpoint/"+this.droneId;

			final DroneInfoDTO droneDto = new DroneInfoDTO(droneData.getDroneId(), this.droneId, droneData.getLatitude(), droneData.getLongitude(), 
					speedInKmH, droneData.getAltitude(), droneData.getVoltage(), droneData.getState(), webSocketURL, droneData.getVideoPort());
			
			this.lastStatus = droneDto;
			
		} catch (Exception e) {
			log.error(e.getMessage());
			log.info("Control Connection with "+droneSocket.getInetAddress().toString()+" Closed");
			close();
		}
	}

	public DroneInfoDTO getDroneLastStatus() {
		return this.lastStatus;
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
		}
	}
}
