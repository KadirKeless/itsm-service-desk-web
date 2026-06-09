package com.example.itsm_final.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationUtilsTest {

    @Test
    void testValidateEmailNullIsInvalid() {
        assertNotNull(ValidationUtils.validateEmail(null), "Boş e-posta için hata dönmeli.");
    }

    @Test
    void testValidateEmailBlankIsInvalid() {
        assertNotNull(ValidationUtils.validateEmail("   "), "Sadece boşluk geçersiz olmalı.");
    }

    @Test
    void testValidateEmailInvalidFormat() {
        assertNotNull(ValidationUtils.validateEmail("gecersiz"), "@ ve alan adı olmadan geçersiz.");
        assertNotNull(ValidationUtils.validateEmail("a@b"), "Tld kısa olunca geçersiz.");
    }

    @Test
    void testValidateEmailValid() {
        assertNull(ValidationUtils.validateEmail("ad@isikun.com"), "Geçerli e-posta null dönmeli.");
        assertNull(ValidationUtils.validateEmail("user.name+tag@example.co.uk"));
    }

    @Test
    void testValidateEmailTrimsWhitespace() {
        assertNull(ValidationUtils.validateEmail("  ad@isikun.com  "));
    }

    @Test
    void testValidatePasswordNullOrEmpty() {
        assertNotNull(ValidationUtils.validatePassword(null));
        assertNotNull(ValidationUtils.validatePassword(""));
    }

    @Test
    void testValidatePasswordTooShort() {
        String msg = ValidationUtils.validatePassword("Aa1!xxx");
        assertNotNull(msg, "7 karakter şifre geçersiz olmalı.");
        assertTrue(msg.contains("8"), "Mesaj uzunluk kuralını belirtmeli.");
    }

    @Test
    void testValidatePasswordMissingUppercase() {
        assertNotNull(ValidationUtils.validatePassword("abcdef1!"), "Büyük harf zorunlu.");
    }

    @Test
    void testValidatePasswordMissingLowercase() {
        assertNotNull(ValidationUtils.validatePassword("ABCDEF1!"), "Küçük harf zorunlu.");
    }

    @Test
    void testValidatePasswordMissingDigit() {
        assertNotNull(ValidationUtils.validatePassword("Abcdefgh!"), "Rakam zorunlu.");
    }

    @Test
    void testValidatePasswordMissingSpecial() {
        assertNotNull(ValidationUtils.validatePassword("Abcdefgh1"), "Özel karakter zorunlu.");
    }

    @Test
    void testValidatePasswordValid() {
        assertNull(ValidationUtils.validatePassword("Valid1!pass"), "Tüm kuralları sağlayan şifre geçerli.");
    }

    @Test
    void testPasswordHintNotEmpty() {
        assertFalse(ValidationUtils.passwordHint().isBlank());
    }
}
