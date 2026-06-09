package com.example.itsm_final.dto;

import com.example.itsm_final.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordDto {

    @NotBlank(message = "Mevcut şifre zorunludur")
    private String currentPassword;

    @NotBlank(message = "Yeni şifre zorunludur")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "Şifre tekrarı zorunludur")
    private String confirmPassword;
}
