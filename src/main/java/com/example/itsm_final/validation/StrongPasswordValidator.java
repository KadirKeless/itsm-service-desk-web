package com.example.itsm_final.validation;

import com.example.itsm_final.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext ctx) {
        String error = ValidationUtils.validatePassword(password);
        if (error == null) {
            return true;
        }
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(error).addConstraintViolation();
        return false;
    }
}
