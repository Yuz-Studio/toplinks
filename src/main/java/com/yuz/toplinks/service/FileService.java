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
import com.yuz.toplinks.entity.TlkCategory;
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
    private final CategoryService categoryService;

    public FileService(TlkFileMapper fileMapper, CloudflareStorageService storageService,
            CategoryService categoryService) {
        this.fileMapper = fileMapper;
        this.storageService = storageService;
        this.categoryService = categoryService;
    }

    /**
     * 上传文件并保存记录到数据库（不支持加密）。
     *
     * @param file       上传的 MultipartFile
     * @param userId     当前用户 ID
     * @param categoryId 所属分类 ID（可为 null）
     * @param request    HTTP 请求（用于获取 IP）
     * @return 保存的 TlkFile 实体
     */
    public TlkFile upload(MultipartFile file, String userId, String categoryId, HttpServletRequest request) throws IOException {
        return upload(file, userId, categoryId, request, false, null);
    }
    
    /**
     * 上传文件并保存记录到数据库（支持加密）。
     *
     * @param file       上传的 MultipartFile
     * @param userId     当前用户 ID
     * @param categoryId 所属分类 ID（可为 null）
     * @param request    HTTP 请求（用于获取 IP）
     * @param encrypt    是否加密
     * @param password   加密密码（encrypt=true 时必需）
     * @return 保存的 TlkFile 实体
     */
    public TlkFile upload(MultipartFile file, String userId, String categoryId, HttpServletRequest request, 
                         boolean encrypt, String password) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IOException("File name cannot be empty");
        }
        // Sanitize
        String safeName = java.nio.file.Paths.get(originalName).getFileName().toString();
        String ext = extractExt(safeName);

        // 计算文件hash并判重（在上传到R2之前）
        byte[] fileBytes = file.getBytes();
        String hash = computeMd5(fileBytes);
        TlkFile existing = findByHash(hash);
        if (existing != null) {
            throw new IOException("File already exists: /file/" + existing.getUid());
        }

        // 加密处理（如果需要）
        byte[] uploadBytes = fileBytes;
        String encryptionKey = null;
        if (encrypt && password != null && !password.isBlank()) {
            EncryptionResult result = encryptFile(fileBytes, password);
            uploadBytes = result.encryptedData;
            encryptionKey = result.keyId;
        }

        String uid = generateUniqueUid();
        String objectKey = "files/" + uid + (ext.isEmpty() ? "" : "." + ext);

        String contentType = MediaTypeFactory.getMediaType(safeName)
                .map(MediaType::toString)
                .orElse("application/octet-stream");

        String cloudUrl = storageService.upload(objectKey, new java.io.ByteArrayInputStream(uploadBytes), uploadBytes.length, contentType);

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
        tlkFile.setCategoryId(categoryId != null && !categoryId.isBlank() ? categoryId
                : detectCategoryId(ext));
        tlkFile.setCreateIp(ip);
        tlkFile.setStatus(BaseEntity.STATUS_ACTIVE);
        tlkFile.setCreateTime(new Date());
        tlkFile.setEncrypted(encrypt && encryptionKey != null);
        tlkFile.setEncryptionKey(encryptionKey);

        fileMapper.insert(tlkFile);
        return tlkFile;
    }

    @Cacheable(value = "fileByUid", key = "#uid")
    public TlkFile findByUid(String uid) {
        return fileMapper.selectOne(new QueryWrapper<TlkFile>().eq("uid", uid));
    }

    /**
     * 根据文件hash查找已存在的文件记录。
     */
    public TlkFile findByHash(String hash) {
        if (hash == null || hash.isBlank()) return null;
        return fileMapper.selectOne(new QueryWrapper<TlkFile>()
                .eq("hash", hash)
                .eq("status", BaseEntity.STATUS_ACTIVE)
                .last("LIMIT 1"));
    }

    public static final int DEFAULT_PAGE_SIZE = 12;

    public List<TlkFile> listByCategoryLimited(String categoryId, int limit) {
        if (limit < 1) limit = DEFAULT_PAGE_SIZE;
        QueryWrapper<TlkFile> qw = new QueryWrapper<TlkFile>()
                .eq("status", BaseEntity.STATUS_ACTIVE)
                .orderByDesc("create_time")
                .last("LIMIT " + limit);
        if (categoryId != null && !categoryId.isBlank()) {
            qw.eq("category_id", categoryId);
        }
        return fileMapper.selectList(qw);
    }

    public List<TlkFile> listByCategory(String categoryId, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = DEFAULT_PAGE_SIZE;
        int offset = (page - 1) * pageSize;
        QueryWrapper<TlkFile> qw = new QueryWrapper<TlkFile>()
                .eq("status", BaseEntity.STATUS_ACTIVE)
                .orderByDesc("create_time")
                .last("LIMIT " + pageSize + " OFFSET " + offset);
        if (categoryId != null && !categoryId.isBlank()) {
            qw.eq("category_id", categoryId);
        }
        return fileMapper.selectList(qw);
    }

    public long countByCategory(String categoryId) {
        QueryWrapper<TlkFile> qw = new QueryWrapper<TlkFile>()
                .eq("status", BaseEntity.STATUS_ACTIVE);
        if (categoryId != null && !categoryId.isBlank()) {
            qw.eq("category_id", categoryId);
        }
        return fileMapper.selectCount(qw);
    }

    // ---- helpers ----

    private String generateUniqueUid() {
        String uid;
        int tries = 0;
        do {
            uid = randomUid();
            tries++;
            if (tries > 100) throw new RuntimeException("Unable to generate a unique UID, please try again later");
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

    /**
     * 根据文件扩展名自动检测并返回对应分类的 ID。
     * 通过查找 icon 字段与文件类型对应的分类实现自动匹配。
     *
     * @param ext 文件扩展名（不含点，小写）
     * @return 匹配分类的 ID，找不到则返回 null
     */
    private String detectCategoryId(String ext) {
        String fileType = resolveFileType(ext);
        String icon = fileTypeToIcon(fileType);
        if (icon == null) return null;
        TlkCategory category = categoryService.findByIcon(icon);
        return category != null ? category.getId() : null;
    }

    /** 根据扩展名返回文件类型标识（复用 TlkFile 中相同的逻辑）。 */
    private static String resolveFileType(String ext) {
        if (ext == null || ext.isBlank()) return "other";
        String lower = ext.toLowerCase();
        if (TlkFile.IMAGE_EXTS.contains(lower))  return "image";
        if (TlkFile.VIDEO_EXTS.contains(lower))  return "video";
        if (TlkFile.AUDIO_EXTS.contains(lower))  return "audio";
        if ("pdf".equals(lower))                  return "pdf";
        if (TlkFile.TEXT_EXTS.contains(lower))   return "text";
        if (TlkFile.DOC_EXTS.contains(lower))    return "document";
        if (TlkFile.MOBI_EXTS.contains(lower))   return "mobi";
        return "other";
    }

    /** 将文件类型标识映射到默认的 Bootstrap icon class 名称。 */
    private static String fileTypeToIcon(String fileType) {
        return switch (fileType) {
            case "image"    -> "bi-image";
            case "video"    -> "bi-play-circle";
            case "audio"    -> "bi-music-note-beamed";
            case "pdf"      -> "bi-file-earmark-pdf";
            case "document" -> "bi-file-earmark-word";
            case "text"     -> "bi-file-earmark-text";
            case "mobi"     -> "bi-book";
            default         -> "bi-file-earmark";
        };
    }
    
    /**
     * 加密结果记录
     */
    private static record EncryptionResult(byte[] encryptedData, String keyId) {}
    
    /**
     * 加密文件数据（使用 AES-256）
     */
    private static EncryptionResult encryptFile(byte[] data, String password) throws IOException {
        try {
            // 生成盐值和 IV
            byte[] salt = new byte[16];
            byte[] iv = new byte[16];
            java.security.SecureRandom random = new java.security.SecureRandom();
            random.nextBytes(salt);
            random.nextBytes(iv);
            
            // 从密码派生密钥
            javax.crypto.spec.PBEKeySpec keySpec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            javax.crypto.SecretKeyFactory keyFactory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            
            // AES 加密
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
            javax.crypto.spec.SecretKeySpec keySpec2 = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec2, ivSpec);
            byte[] encrypted = cipher.doFinal(data);
            
            // 生成密钥 ID（用于标识这个加密配置）
            String keyId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            
            // 组合：salt(16) + iv(16) + encrypted data
            byte[] result = new byte[16 + 16 + encrypted.length];
            System.arraycopy(salt, 0, result, 0, 16);
            System.arraycopy(iv, 0, result, 16, 16);
            System.arraycopy(encrypted, 0, result, 32, encrypted.length);
            
            return new EncryptionResult(result, keyId);
        } catch (Exception e) {
            throw new IOException("Failed to encrypt file: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解密文件数据
     */
    public static byte[] decryptFile(byte[] encryptedData, String password) throws IOException {
        try {
            if (encryptedData.length < 32) {
                throw new IOException("Invalid encrypted data");
            }
            
            // 提取 salt 和 iv
            byte[] salt = new byte[16];
            byte[] iv = new byte[16];
            System.arraycopy(encryptedData, 0, salt, 0, 16);
            System.arraycopy(encryptedData, 16, iv, 0, 16);
            byte[] encrypted = new byte[encryptedData.length - 32];
            System.arraycopy(encryptedData, 32, encrypted, 0, encrypted.length);
            
            // 从密码派生密钥
            javax.crypto.spec.PBEKeySpec keySpec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            javax.crypto.SecretKeyFactory keyFactory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            
            // AES 解密
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
            javax.crypto.spec.SecretKeySpec keySpec2 = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec2, ivSpec);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new IOException("Failed to decrypt file: " + e.getMessage(), e);
        }
    }
}
