package com.quynhlm.dev.be.core;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AppError {
    public enum ErrorCode {
        UNKNOWN,
        DATA_INVALID,
        ACCOUNT_EXIST,
        ACCOUNT_NOT_FOUND,
        ALREADY_EXITES,
        BAD_REQUEST,
        ACCOUNT_DISABLED,
        METHOD_NOT_ALLOWED,
        NOT_FOUND
    }

    private ErrorCode code;
    private String message;

    public AppError(ErrorCode code, String message) {
        this.code = code;
        this.message = message;
    }
}
