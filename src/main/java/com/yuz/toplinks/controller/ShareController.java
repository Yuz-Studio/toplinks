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
import com.yuz.toplinks.entity.TlkShareAuditLog;
import com.yuz.toplinks.service.FileService;
import com.yuz.toplinks.service.ShareService;
import com.yuz.toplinks.service.ShareService.PasswordVerifyResult;
import com.yuz.toplinks.service.CloudflareStorageService;
import com.yuz.toplinks.service.QrCodeService;
import com.yuz.toplinks.service.ShareAuditService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/share")
public class ShareController {
    
    private final ShareService shareService;
    private final FileService fileService;
    private final CloudflareStorageService storageService;
    private final ShareAuditService auditService;
    private final QrCodeService qrCodeService;
    
    public ShareController(ShareService shareService,
                          FileService fileService,
                          CloudflareStorageService storageService,
                          ShareAuditService auditService,
                          QrCodeService qrCodeService) {
        this.shareService = shareService;
        this.fileService = fileService;
        this.storageService = storageService;
        this.auditService = auditService;
        this.qrCodeService = qrCodeService;
    }
    
    /**
     * 获取客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    @GetMapping("/{token}")
    public String viewShare(@PathVariable String token,
                           @RequestParam(required = false) String password,
                           @RequestParam(required = false) String error,
                           Model model,
                           HttpServletRequest request) {
        
        String visitorIp = getClientIp(request);
        
        // 检查 IP 是否被限制
        if (auditService.isIpRateLimited(visitorIp)) {
            model.addAttribute("errorMessage", "Too many failed attempts. Please try again later.");
            return "share/error";
        }
        
        TlkShare share = shareService.findByToken(token);
        if (share == null || !share.isValid()) {
            auditService.logAccess(null, token, visitorIp, 
                TlkShareAuditLog.ACTION_VIEW, false, "Invalid or expired share", null);
            return "share/expired";
        }
        
        TlkFile file = fileService.findByUid(share.getFileId());
        if (file == null) {
            return "share/expired";
        }
        
        // 记录访问日志
        auditService.logAccess(share.getId(), token, visitorIp, 
            TlkShareAuditLog.ACTION_VIEW, true, null, request.getHeader("User-Agent"));
        
        // 生成二维码
        String shareUrl = request.getRequestURL().toString();
        String qrCodeBase64 = qrCodeService.generateShareQrCode(shareUrl);
        model.addAttribute("qrCode", qrCodeBase64);
        
        if (Boolean.TRUE.equals(share.getRequirePassword()) && 
            (password == null || password.isBlank())) {
            model.addAttribute("share", share);
            model.addAttribute("file", file);
            model.addAttribute("error", error);
            return "share/password";
        }
        
        if (Boolean.TRUE.equals(share.getRequirePassword())) {
            PasswordVerifyResult result = shareService.verifyPassword(token, password, visitorIp);
            if (!result.success()) {
                if (result.rateLimited()) {
                    model.addAttribute("errorMessage", result.message());
                    return "share/error";
                }
                return "redirect:/share/" + token + "?error=invalid_password";
            }
        }
        
        model.addAttribute("share", share);
        model.addAttribute("file", file);
        return "share/view";
    }
    
    @GetMapping("/{token}/download")
    public ResponseEntity<Resource> downloadShare(@PathVariable String token,
                                                  @RequestParam(required = false) String password,
                                                  HttpServletRequest request) 
            throws IOException {
        
        String visitorIp = getClientIp(request);
        
        // 检查 IP 是否被限制
        if (auditService.isIpRateLimited(visitorIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        TlkShare share = shareService.findByToken(token);
        if (share == null || !share.isValid()) {
            auditService.logAccess(null, token, visitorIp, 
                TlkShareAuditLog.ACTION_DOWNLOAD, false, "Invalid or expired share", null);
            return ResponseEntity.notFound().build();
        }
        
        if (Boolean.TRUE.equals(share.getRequirePassword())) {
            PasswordVerifyResult result = shareService.verifyPassword(token, password, visitorIp);
            if (!result.success()) {
                auditService.logAccess(share.getId(), token, visitorIp, 
                    TlkShareAuditLog.ACTION_DOWNLOAD, false, result.message(), null);
                if (result.rateLimited()) {
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        
        // 原子增加下载次数
        boolean incremented = shareService.incrementDownloadCount(token);
        if (!incremented) {
            return ResponseEntity.status(HttpStatus.GONE).build(); // 分享已失效
        }
        
        TlkFile file = fileService.findByUid(share.getFileId());
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 记录下载日志
        auditService.logAccess(share.getId(), token, visitorIp, 
            TlkShareAuditLog.ACTION_DOWNLOAD, true, null, request.getHeader("User-Agent"));
        
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
