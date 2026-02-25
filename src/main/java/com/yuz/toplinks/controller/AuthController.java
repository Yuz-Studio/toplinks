package com.yuz.toplinks.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yuz.toplinks.config.SecurityConfig;
import com.yuz.toplinks.service.UserService;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final SecurityConfig securityConfig;

    public AuthController(UserService userService, SecurityConfig securityConfig) {
        this.userService = userService;
        this.securityConfig = securityConfig;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "邮箱或密码错误，请重试。");
        }
        model.addAttribute("googleEnabled", securityConfig.isGoogleOAuthEnabled());
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("googleEnabled", securityConfig.isGoogleOAuthEnabled());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam @Email String email,
            @RequestParam @NotBlank @Size(min = 6, max = 50) String password,
            @RequestParam(required = false) String nickname,
            RedirectAttributes redirectAttributes) {
        try {
            userService.register(email, password, nickname);
            redirectAttributes.addFlashAttribute("successMsg", "注册成功，请登录！");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/auth/register";
        }
    }
}
