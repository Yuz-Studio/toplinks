package com.yuz.toplinks.controller;

import com.yuz.toplinks.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import java.util.logging.Logger;


@Controller
public class FileController {


    private static final Logger logger = Logger.getLogger(FileController.class.getName());


    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/")
    public String index(Model model) throws IOException {
        List<String> files = fileStorageService.listFiles();
        model.addAttribute("files", files);
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "请选择要上传的文件");
            redirectAttributes.addFlashAttribute("alertClass", "alert-warning");
            return "redirect:/";
        }
        try {
            String filename = fileStorageService.storeFile(file);
            redirectAttributes.addFlashAttribute("message", "文件上传成功: " + filename);
            redirectAttributes.addFlashAttribute("alertClass", "alert-success");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "上传失败: " + e.getMessage());
            redirectAttributes.addFlashAttribute("alertClass", "alert-danger");
        }
        return "redirect:/";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = fileStorageService.getFilePath(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                MediaType contentType = MediaTypeFactory.getMediaType(filename)
                        .orElse(MediaType.APPLICATION_OCTET_STREAM);
                String contentDisposition = ContentDisposition.inline()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build().toString();
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                        .contentType(contentType)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (SecurityException e) {

            logger.warning("Path traversal attempt for filename: " + filename);
            return ResponseEntity.badRequest().build();
        } catch (MalformedURLException e) {
            logger.warning("Malformed URL for filename: " + filename);
            return ResponseEntity.badRequest().build();
        }
    }

    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        return "application/octet-stream";
    }
}
