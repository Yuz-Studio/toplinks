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
import com.yuz.toplinks.mapper.TlkShareMapper;

@Service
public class ShareService {
    
    private final TlkShareMapper shareMapper;
    private final PasswordEncoder passwordEncoder;
    
    public ShareService(TlkShareMapper shareMapper, 
                       PasswordEncoder passwordEncoder) {
        this.shareMapper = shareMapper;
        this.passwordEncoder = passwordEncoder;
    }
    
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
     * 验证分享密码
     */
    public boolean verifyPassword(String shareToken, String password) {
        TlkShare share = shareMapper.findByToken(shareToken);
        if (share == null || !share.isValid()) {
            return false;
        }
        
        if (!Boolean.TRUE.equals(share.getRequirePassword())) {
            return true;
        }
        
        if (share.getSharePassword() == null || password == null) {
            return false;
        }
        
        return passwordEncoder.matches(password, share.getSharePassword());
    }
    
    /**
     * 增加下载次数
     */
    @Transactional
    public void incrementDownloadCount(String shareToken) {
        TlkShare share = shareMapper.findByToken(shareToken);
        if (share != null && share.isValid()) {
            share.setDownloadCount(share.getDownloadCount() + 1);
            share.setUpdateTime(new Date());
            shareMapper.updateById(share);
        }
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
     * 清理过期分享
     */
    @Transactional
    public int cleanupExpiredShares() {
        List<TlkShare> expired = shareMapper.findExpired();
        for (TlkShare share : expired) {
            share.setStatus(TlkShare.STATUS_INACTIVE);
            share.setUpdateTime(new Date());
        }
        if (!expired.isEmpty()) {
            expired.forEach(share -> shareMapper.updateById(share));
        }
        return expired.size();
    }
    
    /**
     * 清理已达次数上限的分享
     */
    @Transactional
    public int cleanupMaxDownloadsReached() {
        List<TlkShare> maxReached = shareMapper.findMaxDownloadsReached();
        for (TlkShare share : maxReached) {
            share.setStatus(TlkShare.STATUS_INACTIVE);
            share.setUpdateTime(new Date());
        }
        if (!maxReached.isEmpty()) {
            maxReached.forEach(share -> shareMapper.updateById(share));
        }
        return maxReached.size();
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
