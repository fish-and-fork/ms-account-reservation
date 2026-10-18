package com.example.ms_account_reservation.exception;

import com.example.dto.ErrorResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ApiError {

    CLIENT_NOT_FOUND("CLIENT_NOT_FOUND", "Клиент не найден", HttpStatus.NOT_FOUND)
    ;

    private final String errorCode;
    private final String errorDescription;
    private final HttpStatus statusCode;

    ApiError(String errorCode, String errorDescription, HttpStatus statusCode) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.statusCode = statusCode;
    }

    public ErrorResponse toResponse() {
        return new ErrorResponse()
                .errorCode(this.errorCode)
                .errorDescription(this.errorDescription);
    }
}
