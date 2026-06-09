package com.example.itsm_final.controller;

import com.example.itsm_final.dto.ApiResult;
import com.example.itsm_final.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = AdminApiController.class)
public class AdminApiExceptionHandler {

    @ExceptionHandler(UserService.BusinessException.class)
    public ResponseEntity<ApiResult> handleBusiness(UserService.BusinessException ex) {
        return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
    }

    @ExceptionHandler(UserService.EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResult> handleEmailExists(UserService.EmailAlreadyExistsException ex) {
        return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(ex.getMessage() != null ? ex.getMessage() : "Beklenmeyen bir hata oluştu."));
    }
}
