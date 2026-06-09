package com.example.itsm_final.security;

import com.example.itsm_final.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public Integer getId() {
        return user.getId();
    }

    public String getFullName() {
        return user.getFullName();
    }

    public String getDepartmentName() {
        if (user.getDepartment() == null) {
            return "Departman atanmamış";
        }
        return user.getDepartment().getDepartmentName();
    }

    public String getInitials() {
        return user.getInitials();
    }

    public String getProfilePictureUrl() {
        return user.getProfilePictureUrl();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRole() == null) {
            // Henuz onaylanmamis kullaniciya rol verilmemis olabilir
            return List.of(new SimpleGrantedAuthority("ROLE_PENDING"));
        }
        Integer roleId = user.getRole().getId();
        if (roleId == null) {
            return List.of(new SimpleGrantedAuthority("ROLE_PENDING"));
        }
        return switch (roleId) {
            case 1 -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case 2 -> List.of(new SimpleGrantedAuthority("ROLE_MANAGER"));
            case 3 -> List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
            default -> List.of(new SimpleGrantedAuthority("ROLE_PENDING"));
        };
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // is_approved = false ise login engelle
        return user.isApproved();
    }
}
