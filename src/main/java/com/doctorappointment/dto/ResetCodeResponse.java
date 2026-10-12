package com.doctorappointment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResetCodeResponse(
        String message,
        String verificationCode
) {
    @JsonProperty("resetCode")
    public String getResetCode() {
        return verificationCode;
    }
}
