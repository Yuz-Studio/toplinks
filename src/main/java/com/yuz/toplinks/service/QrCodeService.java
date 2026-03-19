package com.yuz.toplinks.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 二维码生成服务
 * @author yuanzhi
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QrCodeService {
    
    private static final int QR_SIZE = 300;
    private static final int MARGIN = 2;
    
    /**
     * 生成分享链接的二维码（PNG Base64）
     */
    public String generateShareQrCode(String shareUrl) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, MARGIN);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(shareUrl, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            
            BufferedImage image = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < QR_SIZE; x++) {
                for (int y = 0; y < QR_SIZE; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] pngBytes = baos.toByteArray();
            
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
        } catch (WriterException e) {
            log.error("Failed to generate QR code for share: {}", shareUrl, e);
            return null;
        } catch (Exception e) {
            log.error("Failed to generate QR code for share: {}", shareUrl, e);
            return null;
        }
    }
}
