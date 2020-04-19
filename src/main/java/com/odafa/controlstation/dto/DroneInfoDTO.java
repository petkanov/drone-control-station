package com.odafa.controlstation.dto;

public class DroneInfoDTO {
	private String id;
	private String name;
	private double lattitude;
	private double longitude;
	private float speed;
	private float alt;
	private float battery;
	private String state;
	private String webSocketURL;
	private int videoPort;

	public DroneInfoDTO(String id, String name, double lattitude, double longitude, float speed, float alt, 
			            float battery, String state, String webSocketURL, int videoPort) {
		this.id = id;
		this.name = name;
		this.lattitude = lattitude;
		this.longitude = longitude;
		this.speed = speed;
		this.alt = alt;
		this.battery = battery;
		this.state = state;
		this.webSocketURL = webSocketURL;
		this.videoPort = videoPort;
	}

	@Override
	public String toString() {
		return "DroneInfoDTO [id=" + id + ", name=" + name + ", lattitude=" + lattitude + ", longitude=" + longitude
				+ ", speed=" + speed + ", alt=" + alt + ", battery=" + battery + ", state=" + state + ", webSocketURL="
				+ webSocketURL + ", videoPort=" + videoPort + "]";
	}
}
