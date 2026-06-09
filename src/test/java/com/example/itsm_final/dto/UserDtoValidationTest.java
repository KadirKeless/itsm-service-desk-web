package com.example.itsm_final.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Masaustu UserServiceTest icindeki kayit/giris validasyon senaryolari.
 * Web'de bu kurallar DTO + Bean Validation uzerinden uygulanir.
 */
class UserDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testRegisterFailsWhenFirstNameEmpty() {
        RegisterDto dto = validRegisterDto();
        dto.setFirstName("");
        assertFalse(violations(dto).isEmpty(), "Ad boşken kayıt yapılmamalı.");
    }

    @Test
    void testRegisterFailsWhenLastNameEmpty() {
        RegisterDto dto = validRegisterDto();
        dto.setLastName("");
        assertFalse(violations(dto).isEmpty());
    }

    @Test
    void testRegisterFailsWhenEmailInvalid() {
        RegisterDto dto = validRegisterDto();
        dto.setEmail("gecersiz-email");
        assertFalse(violations(dto).isEmpty(), "Geçersiz e-posta ile kayıt olmamalı.");
    }

    @Test
    void testRegisterFailsWhenPasswordWeak() {
        RegisterDto dto = validRegisterDto();
        dto.setPassword("123");
        assertFalse(violations(dto).isEmpty(), "Zayıf şifre ile kayıt olmamalı.");
    }

    @Test
    void testLoginFailsWhenEmailEmpty() {
        LoginDto dto = new LoginDto();
        dto.setEmail("");
        dto.setPassword("Valid1!pass");
        assertFalse(violations(dto).isEmpty(), "Boş e-posta ile giriş geçersiz olmalı.");
    }

    @Test
    void testLoginFailsWhenPasswordEmpty() {
        LoginDto dto = new LoginDto();
        dto.setEmail("ad@isikun.com");
        dto.setPassword("");
        assertFalse(violations(dto).isEmpty(), "Boş şifre ile giriş geçersiz olmalı.");
    }

    @Test
    void testLoginFailsWhenBothEmpty() {
        LoginDto dto = new LoginDto();
        dto.setEmail("  ");
        dto.setPassword("  ");
        assertFalse(violations(dto).isEmpty());
    }

    @Test
    void testAdminCreateUserFailsWhenNamesEmpty() {
        AdminCreateUserDto dto = validAdminCreateDto();
        dto.setFirstName("");
        assertFalse(violations(dto).isEmpty());
    }

    @Test
    void testAdminCreateUserFailsWhenEmailInvalid() {
        AdminCreateUserDto dto = validAdminCreateDto();
        dto.setEmail("bad");
        assertFalse(violations(dto).isEmpty());
    }

    @Test
    void testAdminCreateUserFailsWhenPasswordShort() {
        AdminCreateUserDto dto = validAdminCreateDto();
        dto.setPassword("12345");
        assertFalse(violations(dto).isEmpty());
    }

    @Test
    void testAdminCreateUserFailsWhenRoleNull() {
        AdminCreateUserDto dto = validAdminCreateDto();
        dto.setRoleId(null);
        assertFalse(violations(dto).isEmpty());
    }

    @Test
    void testAdminCreateUserFailsWhenDepartmentNull() {
        AdminCreateUserDto dto = validAdminCreateDto();
        dto.setDepartmentId(null);
        assertFalse(violations(dto).isEmpty());
    }

    @Test
    void testRegisterValidDtoPassesValidation() {
        assertTrue(violations(validRegisterDto()).isEmpty());
    }

    private static RegisterDto validRegisterDto() {
        RegisterDto dto = new RegisterDto();
        dto.setFirstName("Ad");
        dto.setLastName("Soyad");
        dto.setEmail("ad@isikun.com");
        dto.setPassword("Valid1!pass");
        return dto;
    }

    private static AdminCreateUserDto validAdminCreateDto() {
        AdminCreateUserDto dto = new AdminCreateUserDto();
        dto.setFirstName("Ad");
        dto.setLastName("Soyad");
        dto.setEmail("a@b.com");
        dto.setPassword("Valid1!pass");
        dto.setRoleId(2);
        dto.setDepartmentId(1);
        return dto;
    }

    private static <T> Set<ConstraintViolation<T>> violations(T dto) {
        return validator.validate(dto);
    }
}
