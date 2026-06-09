package com.example.itsm_final.dto;

import com.example.itsm_final.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCreateUserDto {

    @NotBlank(message = "Ad zorunludur")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Soyad zorunludur")
    @Size(max = 50)
    private String lastName;

    @NotBlank(message = "E-posta zorunludur")
    @Email(message = "Geçerli bir e-posta giriniz")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Şifre zorunludur")
    @StrongPassword
    private String password;

    @NotNull(message = "Rol seçimi zorunludur")
    private Integer roleId;

    @NotNull(message = "Departman seçimi zorunludur")
    private Integer departmentId;
}
