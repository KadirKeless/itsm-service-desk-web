package com.example.itsm_final.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Yanit yazma / duzenleme form DTO'su.
 * Desktop kurali: min 3 karakter (trimmed).
 */
@Getter
@Setter
public class ReplyDto {

    @NotBlank(message = "Yanıt mesajı boş olamaz")
    @Size(min = 3, max = 2000, message = "Yanıt mesajı en az 3 karakter olmalıdır!")
    private String message;
}
