package com.yuz.toplinks.controller;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.yuz.toplinks.entity.TlkFile;
import com.yuz.toplinks.entity.TlkShare;
import com.yuz.toplinks.service.CloudflareStorageService;
import com.yuz.toplinks.service.FileService;
import com.yuz.toplinks.service.ShareService;

@Controller
@RequestMapping("/share")
public class ShareController {
    
    private final ShareService shareService;
    private final FileService fileService;
    private final CloudflareStorageService storageService;
    
    public ShareController(ShareService shareService,
                          FileService fileService,
                          CloudflareStorageService storageService) {
        this.shareService = shareService;
        this.fileService = fileService;
        this.storageService = storageService;
    }
    
    @GetMapping("/{token}")
    public String viewShare(@PathVariable String token,
                           @RequestParam(required = false) String password,
                           @RequestParam(required = false) String error,
                           Model model) {
        
        TlkShare share = shareService.findByToken(token);
        if (share == null || !share.isValid()) {
            return "share/expired";
        }
        
        TlkFile file = fileService.findByUid(share.getFileId());
        if (file == null) {
            return "share/expired";
        }
        
        if (Boolean.TRUE.equals(share.getRequirePassword()) && 
            (password == null || password.isBlank())) {
            model.addAttribute("share", share);
            model.addAttribute("file", file);
            model.addAttribute("error", error);
            return "share/password";
        }
        
        if (Boolean.TRUE.equals(share.getRequirePassword())) {
            if (!shareService.verifyPassword(token, password)) {
                return "redirect:/share/" + token + "?error=invalid_password";
            }
        }
        
        model.addAttribute("share", share);
        model.addAttribute("file", file);
        return "share/view";
    }
    
    @GetMapping("/{token}/download")
    public ResponseEntity<Resource> downloadShare(@PathVariable String token,
                                                  @RequestParam(required = false) String password) 
            throws IOException {
        
        TlkShare share = shareService.findByToken(token);
        if (share == null || !share.isValid()) {
            return ResponseEntity.notFound().build();
        }
        
        if (Boolean.TRUE.equals(share.getRequirePassword())) {
            if (!shareService.verifyPassword(token, password)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        
        shareService.incrementDownloadCount(token);
        
        TlkFile file = fileService.findByUid(share.getFileId());
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        
        InputStream inputStream = storageService.getInputStream(file.getPath());
        Resource resource = new InputStreamResource(inputStream);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + file.getName() + "\"")
            .contentLength(file.getSize())
            .body(resource);
    }
}
