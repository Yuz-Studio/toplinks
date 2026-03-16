package com.yuz.toplinks.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yuz.toplinks.dto.ShareCreateRequest;
import com.yuz.toplinks.dto.ShareResponse;
import com.yuz.toplinks.entity.TlkShare;
import com.yuz.toplinks.entity.TlkShareAuditLog;
import com.yuz.toplinks.mapper.TlkShareMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShareService {
    
    private final TlkShareMapper shareMapper;
    private final PasswordEncoder passwordEncoder;
    private final ShareAuditService auditService;
    
    /**
     * 创建分享链接
     */
    @Transactional
    public ShareResponse createShare(ShareCreateRequest request, String userId) {
        TlkShare share = new TlkShare();
        share.setId(UUID.randomUUID().toString());
        share.setFileId(request.getFileId());
        share.setShareToken(generateShareToken());
        share.setRequirePassword(request.isRequirePassword());
        share.setCreatedBy(userId);
        share.setDescription(request.getDescription());
        share.setStatus(TlkShare.STATUS_ACTIVE);
        share.setCreateTime(new Date());
        share.setUpdateTime(new Date());
        
        if (request.isRequirePassword() && request.getPassword() != null && !request.getPassword().isBlank()) {
            share.setSharePassword(passwordEncoder.encode(request.getPassword()));
        }
        
        share.setMaxDownloads(request.getMaxDownloads());
        share.setDownloadCount(0);
        share.setExpireTime(request.getExpireTime());
        
        shareMapper.insert(share);
        
        return ShareResponse.builder()
            .id(share.getId())
            .shareUrl(share.getShareUrl())
            .shareToken(share.getShareToken())
            .requirePassword(share.getRequirePassword())
            .maxDownloads(share.getMaxDownloads())
            .downloadCount(share.getDownloadCount())
            .expireTime(share.getExpireTime())
            .description(share.getDescription())
            .isValid(share.isValid())
            .createTime(share.getCreateTime())
            .build();
    }
    
    /**
     * 验证分享密码（带审计和 IP 限制）
     */
    public PasswordVerifyResult verifyPassword(String shareToken, String password, String visitorIp) {
        // 检查 IP 是否被限制
        if (auditService.isIpRateLimited(visitorIp)) {
            return PasswordVerifyResult.rateLimited(auditService.getFailedAttemptsByIp(visitorIp));
        }
        
        TlkShare share = shareMapper.findByToken(shareToken);
        if (share == null || !share.isValid()) {
            return PasswordVerifyResult.invalid();
        }
        
        if (!Boolean.TRUE.equals(share.getRequirePassword())) {
            return PasswordVerifyResult.ok();
        }
        
        if (share.getSharePassword() == null || password == null) {
            auditService.logAccess(share.getId(), shareToken, visitorIp, 
                TlkShareAuditLog.ACTION_PASSWORD_ATTEMPT, false, "Missing password", null);
            return PasswordVerifyResult.failed("Password required");
        }
        
        boolean matches = passwordEncoder.matches(password, share.getSharePassword());
        if (!matches) {
            auditService.logAccess(share.getId(), shareToken, visitorIp, 
                TlkShareAuditLog.ACTION_PASSWORD_ATTEMPT, false, "Wrong password", null);
            return PasswordVerifyResult.failed("Wrong password");
        }
        
        auditService.logAccess(share.getId(), shareToken, visitorIp, 
            TlkShareAuditLog.ACTION_PASSWORD_ATTEMPT, true, null, null);
        return PasswordVerifyResult.ok();
    }
    
    /**
     * 密码验证结果
     */
    public static record PasswordVerifyResult(
        boolean success,
        boolean rateLimited,
        int failedAttempts,
        String message
    ) {
        public static PasswordVerifyResult ok() {
            return new PasswordVerifyResult(true, false, 0, null);
        }
        
        public static PasswordVerifyResult failed(String message) {
            return new PasswordVerifyResult(false, false, 0, message);
        }
        
        public static PasswordVerifyResult invalid() {
            return new PasswordVerifyResult(false, false, 0, "Invalid or expired share");
        }
        
        public static PasswordVerifyResult rateLimited(int failedAttempts) {
            return new PasswordVerifyResult(false, true, failedAttempts, 
                "Too many failed attempts. Please try again later.");
        }
    }
    
    /**
     * 增加下载次数（原子操作，避免并发问题）
     */
    @Transactional
    public boolean incrementDownloadCount(String shareToken) {
        int updated = shareMapper.incrementDownloadCountAtomic(shareToken);
        return updated > 0;
    }
    
    /**
     * 根据 token 查找分享
     */
    public TlkShare findByToken(String token) {
        return shareMapper.findByToken(token);
    }
    
    /**
     * 获取文件的分享列表
     */
    public List<ShareResponse> listByFile(String fileId) {
        List<TlkShare> shares = shareMapper.listByFileId(fileId);
        return shares.stream().map(this::toShareResponse).collect(Collectors.toList());
    }
    
    /**
     * 获取用户的所有分享
     */
    public List<ShareResponse> listByUser(String userId) {
        List<TlkShare> shares = shareMapper.listByUserId(userId);
        return shares.stream().map(this::toShareResponse).collect(Collectors.toList());
    }
    
    /**
     * 删除分享
     */
    @Transactional
    public void deleteShare(String shareId, String userId) {
        TlkShare share = shareMapper.selectById(shareId);
        if (share == null) {
            throw new IllegalArgumentException("Share not found");
        }
        if (!userId.equals(share.getCreatedBy())) {
            throw new IllegalArgumentException("No permission");
        }
        shareMapper.deleteById(shareId);
    }
    
    /**
     * 禁用分享
     */
    @Transactional
    public void disableShare(String shareId, String userId) {
        TlkShare share = shareMapper.selectById(shareId);
        if (share == null) {
            throw new IllegalArgumentException("Share not found");
        }
        if (!userId.equals(share.getCreatedBy())) {
            throw new IllegalArgumentException("No permission");
        }
        share.setStatus(TlkShare.STATUS_INACTIVE);
        share.setUpdateTime(new Date());
        shareMapper.updateById(share);
    }
    
    /**
     * 清理过期分享（批量原子操作）
     */
    @Transactional
    public int cleanupExpiredShares() {
        return shareMapper.cleanupExpiredBatch();
    }
    
    /**
     * 清理已达次数上限的分享（批量原子操作）
     */
    @Transactional
    public int cleanupMaxDownloadsReached() {
        return shareMapper.cleanupMaxDownloadsBatch();
    }
    
    private ShareResponse toShareResponse(TlkShare share) {
        return ShareResponse.builder()
            .id(share.getId())
            .shareUrl(share.getShareUrl())
            .shareToken(share.getShareToken())
            .requirePassword(share.getRequirePassword())
            .maxDownloads(share.getMaxDownloads())
            .downloadCount(share.getDownloadCount())
            .expireTime(share.getExpireTime())
            .description(share.getDescription())
            .isValid(share.isValid())
            .createTime(share.getCreateTime())
            .build();
    }
    
    private String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
