package com.example.itsm_final.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yeni talep formu icin DTO. Hibernate Validator anotasyonlari ile sahaya ozgu
 * kurallar; ek is kurallari TicketService icinde uygulanir (ornek: "kendi
 * departmanina talep acamazsin").
 *
 * Desktop ValidationUtils:
 *   - title  : 5-100 karakter
 *   - desc   : 10-2000 karakter
 *   - dept/category/priority zorunlu
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateTicketDto {

    @NotBlank(message = "Başlık zorunludur")
    @Size(min = 5, max = 100, message = "Başlık 5-100 karakter arasında olmalıdır")
    private String title;

    @NotBlank(message = "Açıklama zorunludur")
    @Size(min = 10, max = 2000, message = "Açıklama 10-2000 karakter arasında olmalıdır")
    private String description;

    @NotNull(message = "Departman seçimi zorunludur")
    private Integer targetDepartmentId;

    @NotNull(message = "Kategori seçimi zorunludur")
    private Integer categoryId;

    @NotNull(message = "Öncelik seçimi zorunludur")
    private Integer priorityId;
}
