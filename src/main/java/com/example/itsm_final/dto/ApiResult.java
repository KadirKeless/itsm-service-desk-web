package com.example.itsm_final.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult {

    private boolean success;
    private String message;
    private Map<String, String> fieldErrors;

    public ApiResult() {
    }

    public ApiResult(boolean success, String message, Map<String, String> fieldErrors) {
        this.success = success;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public static ApiResult ok(String message) {
        return new ApiResult(true, message, null);
    }

    public static ApiResult error(String message) {
        return new ApiResult(false, message, null);
    }

    public static ApiResult validation(String message, Map<String, String> fieldErrors) {
        return new ApiResult(false, message, fieldErrors);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
}
