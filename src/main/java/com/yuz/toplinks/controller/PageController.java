package com.yuz.toplinks.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/about")
    public String about() {
        return "page/about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "page/contact";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "page/privacy";
    }

    @GetMapping("/terms")
    public String terms() {
        return "page/terms";
    }
}
