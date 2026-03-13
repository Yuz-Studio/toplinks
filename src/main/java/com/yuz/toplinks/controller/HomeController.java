package com.yuz.toplinks.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    /** Maximum number of files shown per category section on the default homepage. */
    private static final int SECTION_SIZE = 10;

    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        if (page < 1) page = 1;

        List<TlkCategory> categories = categoryService.listActiveCategories();
        model.addAttribute("categories", categories);

        if (categoryId != null && !categoryId.isBlank()) {
        	return category(categoryId, page, model);
        }
        
        // Default homepage: show limited preview sections for each category
        List<CategorySection> sections = new ArrayList<>();
        for (TlkCategory cat : categories) {
            List<TlkFile> files = fileService.listByCategoryLimited(cat.getId(), SECTION_SIZE);
            sections.add(new CategorySection(cat, files));
        }

        /* Show recent files when no categories exist */
        if (categories.isEmpty()) {
            List<TlkFile> allFiles = fileService.listByCategoryLimited(null, SECTION_SIZE);
            model.addAttribute("recentFiles", allFiles);
        }
        model.addAttribute("categorySections", sections);
        
        return "index";
    }

    @GetMapping("/category/{id}")
    public String category(@PathVariable("id") String id, @RequestParam(defaultValue = "1") int page, Model model) {
    	if (page < 1) page = 1;
    	
    	if (!model.containsAttribute("categories")) {
            List<TlkCategory> categories = categoryService.listActiveCategories();
            model.addAttribute("categories", categories);
    	}
        model.addAttribute("selectedCategory", id);

        // 查找分类名称用于页面标题
        TlkCategory currentCategory = categoryService.getById(id);
        if (currentCategory != null) {
            model.addAttribute("pageTitle", currentCategory.getName());
        }

        // Category-filtered view: paginated file list
        List<TlkFile> files = fileService.listByCategory(id, page, PAGE_SIZE);
        long total = fileService.countByCategory(id);
        long totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE;

        model.addAttribute("files", files);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("total", total);
        
        return "index";
    }
}