package com.yuz.toplinks.service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yuz.toplinks.entity.BaseEntity;
import com.yuz.toplinks.entity.TlkFile;
import com.yuz.toplinks.mapper.TlkFileMapper;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class FileService {

    private static final String UID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int UID_LENGTH = 6;
    private static final Random RANDOM = new Random();

    private final TlkFileMapper fileMapper;
    private final CloudflareStorageService storageService;

    public FileService(TlkFileMapper fileMapper, CloudflareStorageService storageService) {
        this.fileMapper = fileMapper;
        this.storageService = storageService;
    }

    /**
     * 上传文件并保存记录到数据库。
     *
     * @param file       上传的 MultipartFile
     * @param userId     当前用户 ID
     * @param categoryId 所属分类 ID（可为 null）
     * @param request    HTTP 请求（用于获取 IP）
     * @return 保存的 TlkFile 实体
     */
    public TlkFile upload(MultipartFile file, String userId, String categoryId, HttpServletRequest request) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IOException("文件名不能为空");
        }
        // Sanitize
        String safeName = java.nio.file.Paths.get(originalName).getFileName().toString();
        String ext = extractExt(safeName);
        String uid = generateUniqueUid();
        String objectKey = "files/" + uid + (ext.isEmpty() ? "" : "." + ext);

        String contentType = MediaTypeFactory.getMediaType(safeName)
                .map(MediaType::toString)
                .orElse("application/octet-stream");

        String cloudUrl = storageService.upload(objectKey, file.getInputStream(), file.getSize(), contentType);

        String hash = computeMd5(file.getBytes());
        String ip = getClientIp(request);

        TlkFile tlkFile = new TlkFile();
        tlkFile.setId(UUID.randomUUID().toString());
        tlkFile.setName(safeName);
        tlkFile.setPath(objectKey);
        tlkFile.setUid(uid);
        tlkFile.setExt(ext);
        tlkFile.setSize(file.getSize());
        tlkFile.setHash(hash);
        tlkFile.setCloudUrl(cloudUrl);
        tlkFile.setUserId(userId);
        tlkFile.setCategoryId(categoryId);
        tlkFile.setCreateIp(ip);
        tlkFile.setStatus(BaseEntity.STATUS_ACTIVE);
        tlkFile.setCreateTime(new Date());

        fileMapper.insert(tlkFile);
        return tlkFile;
    }

    @Cacheable(value = "fileByUid", key = "#uid")
    public TlkFile findByUid(String uid) {
        return fileMapper.selectOne(new QueryWrapper<TlkFile>().eq("uid", uid));
    }

    @Cacheable(value = "filesByCategory", key = "#categoryId ?: 'all'")
    public List<TlkFile> listByCategory(String categoryId) {
        QueryWrapper<TlkFile> qw = new QueryWrapper<TlkFile>()
                .eq("status", BaseEntity.STATUS_ACTIVE)
                .orderByDesc("create_time")
                .last("LIMIT 20");
        if (categoryId != null && !categoryId.isBlank()) {
            qw.eq("category_id", categoryId);
        }
        return fileMapper.selectList(qw);
    }

    // ---- helpers ----

    private String generateUniqueUid() {
        String uid;
        int tries = 0;
        do {
            uid = randomUid();
            tries++;
            if (tries > 100) throw new RuntimeException("无法生成唯一 UID，请稍后重试");
        } while (fileMapper.selectCount(new QueryWrapper<TlkFile>().eq("uid", uid)) > 0);
        return uid;
    }

    private String randomUid() {
        StringBuilder sb = new StringBuilder(UID_LENGTH);
        for (int i = 0; i < UID_LENGTH; i++) {
            sb.append(UID_CHARS.charAt(RANDOM.nextInt(UID_CHARS.length())));
        }
        return sb.toString();
    }

    private String extractExt(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private String computeMd5(byte[] data) {
        // MD5 used only for file deduplication/integrity checking, not for security
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
