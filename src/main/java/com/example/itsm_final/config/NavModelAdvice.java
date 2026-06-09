package com.example.itsm_final.config;

import com.example.itsm_final.security.CustomUserDetails;
import com.example.itsm_final.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class NavModelAdvice {

    private final UserService userService;

    public NavModelAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void navUserAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof CustomUserDetails principal)) {
            return;
        }
        var user = userService.getById(principal.getId());
        model.addAttribute("navHasAvatar", userService.hasProfilePicture(user));
        model.addAttribute("navAvatarUrl", user.getProfilePictureUrl());
    }
}
