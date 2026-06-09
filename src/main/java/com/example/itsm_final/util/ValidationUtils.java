package com.example.itsm_final.util;

import java.util.regex.Pattern;

public final class ValidationUtils {

    public static final int TICKET_TITLE_MAX_LEN = 100;
    public static final int TICKET_DESCRIPTION_MAX_LEN = 2000;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern HAS_UPPER   = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_LOWER   = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_DIGIT   = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_SPECIAL = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?].*");

    private ValidationUtils() {
    }

    public static String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            return "E-posta adresi boş bırakılamaz.";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Geçerli bir e-posta adresi giriniz. (örnek: ad@sirket.com)";
        }
        return null;
    }

    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Şifre boş bırakılamaz.";
        }
        if (password.length() < 8) {
            return "Şifre en az 8 karakter olmalıdır. (Şu an: " + password.length() + ")";
        }
        if (!HAS_UPPER.matcher(password).matches()) {
            return "Şifre en az bir büyük harf içermelidir. (A-Z)";
        }
        if (!HAS_LOWER.matcher(password).matches()) {
            return "Şifre en az bir küçük harf içermelidir. (a-z)";
        }
        if (!HAS_DIGIT.matcher(password).matches()) {
            return "Şifre en az bir rakam içermelidir. (0-9)";
        }
        if (!HAS_SPECIAL.matcher(password).matches()) {
            return "Şifre en az bir özel karakter içermelidir. (!@#$%^&* vb.)";
        }
        return null;
    }

    public static String passwordPlaceholder() {
        return "••••••••";
    }

    public static String passwordHint() {
        return "En az 8 karakter · Büyük/küçük harf · Rakam · Özel karakter (!@#$ vb.)";
    }

    public static String validateTicketTitle(String title) {
        if (title == null || title.trim().length() < 5) {
            return "Talep başlığı en az 5 karakter olmalıdır!";
        }
        if (title.length() > TICKET_TITLE_MAX_LEN) {
            return "Talep başlığı en fazla " + TICKET_TITLE_MAX_LEN + " karakter olabilir.";
        }
        return null;
    }

    public static String validateTicketDescription(String description) {
        if (description == null || description.trim().length() < 10) {
            return "Lütfen sorununuzu daha detaylı açıklayınız (En az 10 karakter)!";
        }
        if (description.length() > TICKET_DESCRIPTION_MAX_LEN) {
            return "Açıklama en fazla " + TICKET_DESCRIPTION_MAX_LEN + " karakter olabilir.";
        }
        return null;
    }
}
