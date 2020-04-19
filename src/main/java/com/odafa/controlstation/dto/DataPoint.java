package com.odafa.controlstation.dto;

public class DataPoint {
	private String lat;
	private String lng;
	private float speed;
	private float height;
	private int action;

	public DataPoint(String lat, String lng, float speed, float height, int action) {
		this.lat = lat;
		this.lng = lng;
		this.speed = speed;
		this.height = height;
		this.action = action;
	}

	public String getLat() {
		return lat;
	}

	public String getLng() {
		return lng;
	}

	public float getSpeed() {
		return speed;
	}

	public float getHeight() {
		return height;
	}

	public int getAction() {
		return action;
	}

	@Override
	public String toString() {
		return "DataPoint [lat=" + lat + ", lng=" + lng + ", speed=" + speed + ", height=" + height + ", action="
				+ action + "]";
	}
}
