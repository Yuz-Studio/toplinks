package com.yuz.toplinks.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Cloudflare R2 存储服务。
 * 当 R2 凭证未配置时自动降级到本地文件存储。
 */
@Service
public class CloudflareStorageService {

    private final String bucket;
    private final String publicUrl;
    private final boolean r2Enabled;
    private final S3Client s3Client;
    private final FileStorageService localStorage;

    public CloudflareStorageService(
            @Value("${cloudflare.r2.account-id:}") String accountId,
            @Value("${cloudflare.r2.access-key:}") String accessKey,
            @Value("${cloudflare.r2.secret-key:}") String secretKey,
            @Value("${cloudflare.r2.bucket:toplinks}") String bucket,
            @Value("${cloudflare.r2.public-url:}") String publicUrl,
            FileStorageService localStorage) {

        this.bucket = bucket;
        this.publicUrl = publicUrl;
        this.localStorage = localStorage;
        this.r2Enabled = !accountId.isBlank() && !accessKey.isBlank() && !secretKey.isBlank();

        if (r2Enabled) {
            this.s3Client = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                    .region(Region.of("auto"))
                    .serviceConfiguration(s -> s.pathStyleAccessEnabled(true))
                    .build();
        } else {
            this.s3Client = null;
        }
    }

    /**
     * 上传文件并返回公开访问 URL。
     *
     * @param objectKey   对象键，例如 "files/abc123.pdf"
     * @param inputStream 文件输入流
     * @param size        字节数
     * @param contentType MIME 类型
     * @return 公开访问 URL
     */
    public String upload(String objectKey, InputStream inputStream, long size, String contentType) throws IOException {
        if (r2Enabled) {
            // Read all bytes into memory so the SDK uses standard (non-chunked) SigV4 signing.
            // Cloudflare R2 does not support STREAMING-AWS4-HMAC-SHA256-PAYLOAD chunked signing,
            // which fromInputStream() uses. The caller is responsible for enforcing an upload
            // size limit (e.g. spring.servlet.multipart.max-file-size) to bound memory usage.
            if (size < 0) {
                throw new IOException("Invalid file size: " + size);
            }
            byte[] bytes = inputStream.readAllBytes();
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(contentType)
                            .contentLength((long) bytes.length)
                            .build(),
                    RequestBody.fromBytes(bytes));
            return buildPublicUrl(objectKey);
        } else {
            // 降级：存储到本地并返回本地访问路径
            String filename = objectKey.contains("/") ? objectKey.substring(objectKey.lastIndexOf('/') + 1) : objectKey;
            localStorage.storeWithName(inputStream, filename);
            return "/files/" + filename;
        }
    }

    /**
     * 获取对象的公开访问 URL（用于图片直接重定向）。
     */
    public String getPublicUrl(String objectKey) {
        if (r2Enabled) {
            return buildPublicUrl(objectKey);
        }
        String filename = objectKey.contains("/") ? objectKey.substring(objectKey.lastIndexOf('/') + 1) : objectKey;
        return "/files/" + filename;
    }

    public boolean isR2Enabled() {
        return r2Enabled;
    }

    private String buildPublicUrl(String objectKey) {
        if (publicUrl.isBlank()) {
            // publicUrl must be configured when R2 is enabled; log a warning and return object key as path
            java.util.logging.Logger.getLogger(getClass().getName())
                    .warning("cloudflare.r2.public-url is not set; download URLs will be incomplete.");
            return objectKey;
        }
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        return base + "/" + objectKey;
    }
}
