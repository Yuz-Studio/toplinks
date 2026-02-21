package com.yuz.toplinks.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.yuz.toplinks.entity.TlkCategory;
import com.yuz.toplinks.entity.TlkFile;
import com.yuz.toplinks.service.CategoryService;
import com.yuz.toplinks.service.FileService;

@Controller
public class HomeController {

    private final CategoryService categoryService;
    private final FileService fileService;

    public HomeController(CategoryService categoryService, FileService fileService) {
        this.categoryService = categoryService;
        this.fileService = fileService;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String categoryId,
            Model model) {

        List<TlkCategory> categories = categoryService.listActiveCategories();
        List<TlkFile> files = fileService.listByCategory(categoryId);

        model.addAttribute("categories", categories);
        model.addAttribute("files", files);
        model.addAttribute("selectedCategory", categoryId);

        return "index";
    }
}
