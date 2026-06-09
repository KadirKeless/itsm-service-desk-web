package com.example.itsm_final.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Ad alanı zorunludur")
    @Size(max = 50, message = "Ad en fazla 50 karakter olabilir")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Soyad alanı zorunludur")
    @Size(max = 50, message = "Soyad en fazla 50 karakter olabilir")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "E-posta zorunludur")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = 100, message = "E-posta en fazla 100 karakter olabilir")
    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @NotBlank(message = "Şifre zorunludur")
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "is_approved", nullable = false)
    private boolean approved = false;

    @Column(name = "profile_picture", length = 255)
    private String profilePicture;

    public String getFullName() {
        return (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
    }

    /** Web URL: /assets-avatars/avatar_{id}.png */
    public String getProfilePictureUrl() {
        if (profilePicture == null || profilePicture.isBlank()) {
            return null;
        }
        return profilePicture.startsWith("/") ? profilePicture : "/" + profilePicture;
    }

    public String getInitials() {
        String f = (firstName != null && !firstName.isEmpty())
                ? firstName.substring(0, 1).toUpperCase() : "";
        String l = (lastName != null && !lastName.isEmpty())
                ? lastName.substring(0, 1).toUpperCase() : "";
        return f + l;
    }
}
