package com.yuz.toplinks.service;

import com.yuz.toplinks.entity.TlkCategory;
import com.yuz.toplinks.mapper.TlkFileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 验证上传文件时自动根据文件类型分配分类（不需要用户手动设置）。
 */
@ExtendWith(MockitoExtension.class)
class FileServiceAutoDetectTest {

    @Mock
    private TlkFileMapper fileMapper;

    @Mock
    private CloudflareStorageService storageService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private FileService fileService;

    @Test
    void imageFileAutoAssignsImageCategory() {
        TlkCategory imageCategory = new TlkCategory();
        imageCategory.setId("cat-001");
        imageCategory.setIcon("bi-image");

        when(categoryService.findByIcon(eq("bi-image"))).thenReturn(imageCategory);

        // Use reflection to call the private detectCategoryId method
        String categoryId = invokeDet("jpg");

        assertEquals("cat-001", categoryId);
        verify(categoryService).findByIcon("bi-image");
    }

    @Test
    void pdfFileAutoAssignsPdfCategory() {
        TlkCategory pdfCategory = new TlkCategory();
        pdfCategory.setId("cat-003");
        pdfCategory.setIcon("bi-file-earmark-pdf");

        when(categoryService.findByIcon(eq("bi-file-earmark-pdf"))).thenReturn(pdfCategory);

        String categoryId = invokeDet("pdf");

        assertEquals("cat-003", categoryId);
    }

    @Test
    void videoFileAutoAssignsVideoCategory() {
        TlkCategory videoCategory = new TlkCategory();
        videoCategory.setId("cat-005");
        videoCategory.setIcon("bi-play-circle");

        when(categoryService.findByIcon(eq("bi-play-circle"))).thenReturn(videoCategory);

        String categoryId = invokeDet("mp4");

        assertEquals("cat-005", categoryId);
    }

    @Test
    void unknownExtensionFallsBackToOtherCategory() {
        TlkCategory otherCategory = new TlkCategory();
        otherCategory.setId("cat-006");
        otherCategory.setIcon("bi-file-earmark");

        when(categoryService.findByIcon(eq("bi-file-earmark"))).thenReturn(otherCategory);

        String categoryId = invokeDet("xyz");

        assertEquals("cat-006", categoryId);
    }

    @Test
    void noCategoryFoundReturnsNull() {
        when(categoryService.findByIcon(any())).thenReturn(null);

        String categoryId = invokeDet("jpg");

        assertNull(categoryId);
    }

    /** 通过反射调用 private detectCategoryId 方法。 */
    private String invokeDet(String ext) {
        try {
            java.lang.reflect.Method m = FileService.class.getDeclaredMethod("detectCategoryId", String.class);
            m.setAccessible(true);
            return (String) m.invoke(fileService, ext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
