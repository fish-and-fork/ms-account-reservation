package com.example.ms_account_reservation.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ApiError apiError;

    public ApiException(ApiError apiError) {
        super(apiError.getErrorDescription());
        this.apiError = apiError;
    }

    public ApiException(ApiError apiError, String details) {
        super(details); // для более детального лога
        this.apiError = apiError;
    }
}