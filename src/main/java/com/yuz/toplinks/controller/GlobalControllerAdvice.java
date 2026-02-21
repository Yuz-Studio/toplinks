package com.yuz.toplinks.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.yuz.toplinks.entity.SysUser;
import com.yuz.toplinks.service.UserService;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserService userService;

    public GlobalControllerAdvice(UserService userService) {
        this.userService = userService;
    }

    /**
     * Makes {@code currentUser} available in every Thymeleaf template.
     * Returns null when unauthenticated.
     */
    @ModelAttribute("currentUser")
    public SysUser currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email;
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }
        if (email == null) return null;
        return userService.findByEmail(email);
    }
}
