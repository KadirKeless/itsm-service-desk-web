package com.example.itsm_final.config;

import com.example.itsm_final.service.ProfilePictureService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web uygulamasinin assets-avatars/ klasorunu statik olarak servis eder.
 * DB'deki yol: assets-avatars/avatar_{id}.png -> URL: /assets-avatars/avatar_{id}.png
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ProfilePictureService profilePictureService;

    public WebConfig(ProfilePictureService profilePictureService) {
        this.profilePictureService = profilePictureService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = profilePictureService.getAvatarDirectory()
                .toUri()
                .toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/assets-avatars/**")
                .addResourceLocations(location);
    }
}
