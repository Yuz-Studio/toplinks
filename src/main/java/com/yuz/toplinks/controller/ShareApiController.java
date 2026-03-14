package com.yuz.toplinks.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yuz.toplinks.dto.ShareCreateRequest;
import com.yuz.toplinks.dto.ShareResponse;
import com.yuz.toplinks.service.ShareService;

@RestController
@RequestMapping("/api/share")
public class ShareApiController {
    
    private final ShareService shareService;
    
    public ShareApiController(ShareService shareService) {
        this.shareService = shareService;
    }
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createShare(
            @RequestBody ShareCreateRequest request,
            Principal principal) {
        
        try {
            String userId = principal.getName();
            ShareResponse share = shareService.createShare(request, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", share);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> listShares(
            @RequestParam String fileId,
            Principal principal) {
        
        String userId = principal.getName();
        List<ShareResponse> shares = shareService.listByFile(fileId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", shares);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> listMyShares(Principal principal) {
        
        String userId = principal.getName();
        List<ShareResponse> shares = shareService.listByUser(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", shares);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deleteShare(
            @PathVariable String id,
            Principal principal) {
        
        try {
            String userId = principal.getName();
            shareService.deleteShare(id, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Share deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/{id}/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> disableShare(
            @PathVariable String id,
            Principal principal) {
        
        try {
            String userId = principal.getName();
            shareService.disableShare(id, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Share disabled successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
