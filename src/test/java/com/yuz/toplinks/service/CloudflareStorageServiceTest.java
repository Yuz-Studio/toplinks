package com.yuz.toplinks.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证 CloudflareStorageService 上传时使用 fromBytes（非 chunked streaming），
 * 以兼容 Cloudflare R2。
 */
@ExtendWith(MockitoExtension.class)
class CloudflareStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private FileStorageService localStorage;

    private CloudflareStorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        // Construct with r2Enabled=true by injecting a real instance then replacing s3Client
        storageService = new CloudflareStorageService(
                "test-account",
                "test-access-key",
                "test-secret-key",
                "test-bucket",
                "https://pub.example.com",
                localStorage);

        // Inject mock S3Client via reflection
        Field field = CloudflareStorageService.class.getDeclaredField("s3Client");
        field.setAccessible(true);
        field.set(storageService, s3Client);

    }

    @Test
    void uploadUsesBytesNotStreamingPayload() throws IOException {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        byte[] content = "hello cloudflare r2".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);

        String url = storageService.upload("files/test.txt", inputStream, content.length, "text/plain");

        // Verify putObject was called once
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        ArgumentCaptor<PutObjectRequest> reqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(reqCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest req = reqCaptor.getValue();
        assertEquals("test-bucket", req.bucket());
        assertEquals("files/test.txt", req.key());
        assertEquals("text/plain", req.contentType());

        // RequestBody built from bytes has a known content length equal to byte array length
        RequestBody body = bodyCaptor.getValue();
        assertEquals(content.length, body.optionalContentLength().orElseThrow());

        // URL should be built from publicUrl + objectKey
        assertEquals("https://pub.example.com/files/test.txt", url);
    }

    @Test
    void r2DisabledFallsBackToLocalStorage() throws IOException {
        CloudflareStorageService disabled = new CloudflareStorageService(
                "", "", "", "bucket", "", localStorage);

        byte[] content = "local".getBytes();
        String url = disabled.upload("files/local.txt", new ByteArrayInputStream(content), content.length, "text/plain");

        verify(localStorage, times(1)).storeWithName(any(), eq("local.txt"));
        assertEquals("/files/local.txt", url);
        verifyNoInteractions(s3Client);
    }
}
