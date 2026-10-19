package com.example.ms_account_reservation.exception;

import com.example.dto.ErrorResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ApiError {

    VALIDATION_ERROR("VALIDATION_ERROR", "Невалидные данные запроса", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_PARAMETER("INVALID_REQUEST_PARAMETER", "Неверный формат параметра запроса", HttpStatus.BAD_REQUEST),
    CLIENT_NOT_FOUND("CLIENT_NOT_FOUND", "Клиент не найден", HttpStatus.NOT_FOUND),
    CLIENT_MDM_EXISTS("CLIENT_MDM_EXISTS", "Клиент с таким mdmId уже существует", HttpStatus.CONFLICT),
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
