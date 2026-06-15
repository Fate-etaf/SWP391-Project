package com.swp5.library_management.librarian.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Generates QR code PNG images on demand.
 *
 * <p>Usage: the encoded content of each BookCopy QR is its {@code copyId}.
 * The image is rendered as raw PNG bytes (suitable for an HTTP response)
 * or as a Base64 data-URI (suitable for embedding in HTML).
 */
@Service
public class QrCodeService {

    private static final int DEFAULT_SIZE = 250; // px

    /**
     * Returns a QR code PNG as a byte array for the given text.
     *
     * @param text   the string to encode (e.g. copyId)
     * @param sizePx width & height in pixels
     */
    public byte[] generatePng(String text, int sizePx) {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 1
        );
        try {
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code for: " + text, e);
        }
    }

    /**
     * Convenience overload using the default size (250 px).
     */
    public byte[] generatePng(String text) {
        return generatePng(text, DEFAULT_SIZE);
    }
}
