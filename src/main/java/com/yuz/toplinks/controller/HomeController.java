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

    private static final int PAGE_SIZE = FileService.DEFAULT_PAGE_SIZE;

    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        if (page < 1) page = 1;

        List<TlkCategory> categories = categoryService.listActiveCategories();
        List<TlkFile> files = fileService.listByCategory(categoryId, page, PAGE_SIZE);
        long total = fileService.countByCategory(categoryId);
        long totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE;

        model.addAttribute("categories", categories);
        model.addAttribute("files", files);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("total", total);

        return "index";
    }
}
