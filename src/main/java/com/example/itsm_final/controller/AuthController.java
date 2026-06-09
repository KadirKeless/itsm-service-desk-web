package com.example.itsm_final.controller;

import com.example.itsm_final.dto.LoginDto;
import com.example.itsm_final.dto.RegisterDto;
import com.example.itsm_final.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "registered", required = false) String registered,
                            Model model) {
        model.addAttribute("loginDto", new LoginDto());
        if (error != null && !error.isBlank()) {
            model.addAttribute("errorMessage", error);
        }
        if (registered != null) {
            model.addAttribute("successMessage",
                    "Kayıt başarılı! Hesabınız Admin onayından sonra aktif olacaktır.");
        }
        return "login";
    }


    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterDto());
        }
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("registerDto") RegisterDto registerDto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttrs) {

        // 1) Sahaya ozgu validation hatalari varsa formu yeniden goster
        if (bindingResult.hasErrors()) {
            return "register";
        }

        // 2) Servis katmaninda business validation (e-posta benzersizligi)
        try {
            userService.register(registerDto);
        } catch (UserService.EmailAlreadyExistsException ex) {
            // DTO'nun "email" field'ina hata bagla — th:errors ekranda gosterir
            bindingResult.addError(new FieldError(
                    "registerDto", "email",
                    registerDto.getEmail(), false, null, null,
                    "Bu e-posta adresi zaten kayıtlı"));
            return "register";
        }

        // 3) Basari: login sayfasina yonlendir + flash mesaj
        redirectAttrs.addAttribute("registered", "true");
        return "redirect:/login";
    }
}
