package com.example.itsm_final.security;

import com.example.itsm_final.model.User;
import com.example.itsm_final.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Kullanıcı bulunamadı: " + email));
        return new CustomUserDetails(user);
    }

    /**
     * DelegatingPasswordEncoder dogrulamadan sonra bu metodu cagirir:
     * - Legacy plain-text sifre eslesirse, yeni sifre BCrypt'li haliyle gelir
     * - Biz de DB'ye yazariz; bir sonraki login artik BCrypt uzerinden dogrulanir
     */
    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails userDetails, String newEncodedPassword) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Şifre güncellenemedi, kullanıcı yok: " + userDetails.getUsername()));
        user.setPassword(newEncodedPassword);
        userRepository.save(user);
        return new CustomUserDetails(user);
    }
}
