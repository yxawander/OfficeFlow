package com.officeflow.attendance.dto;

public record CheckOutRequest(
        String remark,
        Double latitude,
        Double longitude,
        Double accuracyMeters
) {
}
