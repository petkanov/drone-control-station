package com.odafa.controlstation.droneserver;

import java.util.List;

import com.odafa.controlstation.droneserver.ProtoData.MissionData.Builder;
import com.odafa.controlstation.dto.DataPoint;

public class CommandBuilder {

	public static byte[] translateCommand(int commandCode) {
		return ProtoData.Command.newBuilder().setCode(commandCode).build().toByteArray();
	}

	public static byte[] translateMissionData(List<DataPoint> dataPoints) {
		final Builder missionData = ProtoData.MissionData.newBuilder();
		for (DataPoint point : dataPoints) {
			missionData.addPoint( ProtoData.DataPoint.newBuilder()
					                                 .setLatitude(point.getLat())
					                                 .setLongitude(point.getLng())
					                                 .setSpeed(point.getSpeed())
					                                 .setAltitude(point.getHeight())
					                                 .setAction(point.getAction())
					                                 .build());
		}
		return ProtoData.Command.newBuilder().setCode(14)
				                             .setPayload( missionData.build().toByteString())
				                             .build().toByteArray();
	}
}
