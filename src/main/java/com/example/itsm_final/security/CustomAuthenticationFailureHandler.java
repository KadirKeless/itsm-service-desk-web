package com.example.itsm_final.security;

import com.example.itsm_final.model.User;
import com.example.itsm_final.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UserRepository userRepository;

    public CustomAuthenticationFailureHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String email = request.getParameter("email");
        String message;

        if (exception instanceof DisabledException && email != null) {
            Optional<User> opt = userRepository.findByEmail(email);
            if (opt.isPresent()) {
                User u = opt.get();
                boolean assigned = u.getRole() != null && u.getDepartment() != null;
                if (assigned) {
                    message = "Hesabınız bir yönetici tarafından dondurulmuştur. "
                            + "Erişim için sistem yöneticisiyle iletişime geçiniz.";
                } else {
                    message = "Hesabınız henüz Admin tarafından onaylanmamış. "
                            + "Lütfen daha sonra tekrar deneyiniz.";
                }
            } else {
                message = "E-posta adresi veya şifre hatalı!";
            }
        } else {
            message = "E-posta adresi veya şifre hatalı!";
        }

        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        setDefaultFailureUrl("/login?error=" + encoded);
        super.onAuthenticationFailure(request, response, exception);
    }
}
