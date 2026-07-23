package com.officeflow.attendance.dto;

public record CheckInRequest(
        String remark,
        Double latitude,
        Double longitude,
        Double accuracyMeters
) {
}
