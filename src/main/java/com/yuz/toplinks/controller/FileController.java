package com.yuz.toplinks.controller;

import java.io.IOException;
import java.util.logging.Logger;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yuz.toplinks.entity.TlkFile;
import com.yuz.toplinks.entity.SysUser;
import com.yuz.toplinks.service.FileService;
import com.yuz.toplinks.service.FileStorageService;
import com.yuz.toplinks.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class FileController {

    private static final Logger logger = Logger.getLogger(FileController.class.getName());

    private final FileService fileService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public FileController(FileService fileService, UserService userService,
            FileStorageService fileStorageService) {
        this.fileService = fileService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    /** 文件上传页面（需要登录） */
    @GetMapping("/upload")
    public String uploadPage() {
        return "file/upload";
    }

    /** 处理文件上传 */
    @PostMapping("/upload")
    public String handleUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "请选择要上传的文件");
            redirectAttributes.addFlashAttribute("alertClass", "alert-warning");
            return "redirect:/upload";
        }

        try {
            String userId = resolveUserId(authentication);
            TlkFile tlkFile = fileService.upload(file, userId, categoryId, request);
            redirectAttributes.addFlashAttribute("message", "文件上传成功！访问地址：/f/" + tlkFile.getUid());
            redirectAttributes.addFlashAttribute("alertClass", "alert-success");
            return "redirect:/f/" + tlkFile.getUid();
        } catch (IOException e) {
            logger.warning("File upload failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("message", "上传失败：" + e.getMessage());
            redirectAttributes.addFlashAttribute("alertClass", "alert-danger");
            return "redirect:/upload";
        }
    }

    /** 文件详情页面（公开访问） */
    @GetMapping("/f/{uid}")
    public String fileDetail(@PathVariable String uid, Model model) {
        TlkFile file = fileService.findByUid(uid);
        if (file == null) {
            return "error/404";
        }
        model.addAttribute("file", file);
        return "file/detail";
    }

    /**
     * 文件下载。
     * 图片类直接重定向到 Cloudflare URL，避免占用服务器带宽。
     * 其他文件同样通过重定向让 Cloudflare 直接传输。
     */
    @GetMapping("/f/{uid}/download")
    public ResponseEntity<Void> downloadFile(@PathVariable String uid) {
        TlkFile file = fileService.findByUid(uid);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        String cloudUrl = file.getCloudUrl();
        if (cloudUrl == null || cloudUrl.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, cloudUrl)
                .build();
    }

    // ---- legacy local-file endpoint (fallback when R2 is disabled) ----

    @GetMapping("/files/{filename:.+}")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> serveLocalFile(
            @PathVariable String filename) {
        try {
            java.nio.file.Path filePath = fileStorageService.getFilePath(filename);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                org.springframework.http.MediaType contentType =
                        org.springframework.http.MediaTypeFactory.getMediaType(filename)
                                .orElse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
                String disposition = org.springframework.http.ContentDisposition.inline()
                        .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
                        .build().toString();
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                        .contentType(contentType)
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.badRequest().build();
        } catch (java.net.MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ---- helpers ----

    private String resolveUserId(Authentication authentication) {
        if (authentication == null) return null;
        String email;
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }
        SysUser user = userService.findByEmail(email);
        return user != null ? user.getId() : null;
    }
}

