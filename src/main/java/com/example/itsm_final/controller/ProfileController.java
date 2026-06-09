package com.example.itsm_final.controller;

import com.example.itsm_final.dto.ChangePasswordDto;
import com.example.itsm_final.model.Role;
import com.example.itsm_final.security.CustomUserDetails;
import com.example.itsm_final.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * /profile sayfasi: kullanici kendi bilgilerini gorur ve sifresini degistirir.
 * Desktop ProfileUI'daki davranis bire bir korunmustur.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String profile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        var user = userService.getById(principal.getUser().getId());
        model.addAttribute("user", user);
        model.addAttribute("hasProfilePicture", userService.hasProfilePicture(user));
        model.addAttribute("isAdmin", user.getRole() != null
                && Role.ADMIN_ID.equals(user.getRole().getId()));
        model.addAttribute("roleName", user.getRole() != null
                ? user.getRole().getRoleName() : "Belirtilmemiş");
        model.addAttribute("departmentName", user.getDepartment() != null
                ? user.getDepartment().getDepartmentName() : "Atanmamış");

        if (!model.containsAttribute("passwordDto")) {
            model.addAttribute("passwordDto", new ChangePasswordDto());
        }
        return "profile";
    }

    /** Yalnizca admin kendi profil resmini guncelleyebilir (masaustu ProfileUI). */
    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("profilePicture") MultipartFile profilePicture,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttrs) {
        var user = principal.getUser();
        if (user.getRole() == null || !Role.ADMIN_ID.equals(user.getRole().getId())) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Profil resmi sistem yöneticisi tarafından güncellenebilir.");
            return "redirect:/profile";
        }
        try {
            userService.updateProfilePicture(user.getId(), profilePicture);
            redirectAttrs.addFlashAttribute("successMessage", "Profil resminiz guncellendi.");
        } catch (UserService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/avatar/remove")
    public String removeAvatar(@AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttrs) {
        var user = principal.getUser();
        if (user.getRole() == null || !Role.ADMIN_ID.equals(user.getRole().getId())) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Profil resmi sistem yöneticisi tarafından güncellenebilir.");
            return "redirect:/profile";
        }
        userService.removeProfilePicture(user.getId());
        redirectAttrs.addFlashAttribute("successMessage", "Profil resminiz kaldirildi.");
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordDto") ChangePasswordDto dto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.passwordDto", bindingResult);
            redirectAttrs.addFlashAttribute("passwordDto", dto);
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Şifre formundaki hataları düzeltiniz.");
            return "redirect:/profile";
        }
        try {
            userService.changePassword(principal.getUser().getId(), dto);
            redirectAttrs.addFlashAttribute("successMessage", "Şifreniz başarıyla güncellendi!");
        } catch (UserService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/profile";
    }
}
