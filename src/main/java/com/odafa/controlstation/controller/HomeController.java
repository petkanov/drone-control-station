package com.odafa.controlstation.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.odafa.controlstation.droneserver.DroneCloudServer;
import com.odafa.controlstation.droneserver.DroneHandler;
import com.odafa.controlstation.dto.DataPoint;
import com.odafa.controlstation.dto.DroneInfoDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class HomeController {
	
	@Autowired 
	private DroneCloudServer droneServer;
	
	@GetMapping("/")
	public String commandCenter(Model model) {
		
		model.addAttribute("localIp", getGlobalIpAddress());
		
		// <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyAKvdklCWlOEBirFt_qu5Wy2EMjKwz0EXo">
		
		return "index";
	}

	@ResponseBody
	@PostMapping("/sendCommand")
	public String sendCommand(@RequestParam("droneId") String droneId, @RequestParam("commandCode") String commandCode) throws IOException {
		try {
			if(commandCode == null || commandCode.trim().length() < 1) {
				return "EMPTY DATA";
			}
			droneServer.sendMessageFromUserIdToDrone(droneId, Integer.parseInt(commandCode));
			
			return "OK";
		} catch (Exception e) {
			log.error(e.getMessage());
			return "";
		}
	}
	
	@ResponseBody
	@PostMapping("/startMission")
	public String startMission( @RequestParam("points") String points, @RequestParam("droneId") String droneId) throws IOException {
		try {
			if(points == null || points.trim().length() < 5) {
				return "EMPTY DATA";
			}
			final Gson gson = new Gson();
			final List<DataPoint> deserializedPoints = new ArrayList<>();
			
			for (Object obj : gson.fromJson(points, List.class)) {
				deserializedPoints.add(gson.fromJson(obj.toString(), DataPoint.class));
			}
			droneServer.sendMissionDataToDrone(droneId, deserializedPoints);
		}catch(Exception e) {
			log.error(e.getMessage());
		}
		return "OK";
	}

	@ResponseBody
	@GetMapping("/updateSystemInfo")
	public String updateSystemInfo() throws IOException {
		final Gson gson = new Gson();
		final List<DroneInfoDTO> drones = new ArrayList<>();
		
		try {
			final Collection<DroneHandler> droneHandlers = droneServer.getDronesHandlers();
			
			if (droneHandlers == null) {
				return gson.toJson(drones);
			}
			
			for(DroneHandler handler : droneHandlers) {
				drones.add(handler.getDroneLastStatus());
			}
			return gson.toJson(drones);
			
		} catch (Exception e) {
			log.error(e.getMessage());
			return "";
		}
	}
	
	private String getGlobalIpAddress() {
		String ip = "";
		try {
			final URL whatismyip = new URL("http://checkip.amazonaws.com");
			final BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			ip = in.readLine();
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return ip;
	}
}

