package com.swp5.library_management.controller;

import com.swp5.library_management.service.QrCodeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint that streams QR code images for BookCopy records.
 *
 * <p>Usage in HTML/Thymeleaf:
 * <pre>{@code
 *   <img th:src="@{/api/qr/{id}(id=${copy.copyId})}" alt="QR Code"/>
 * }</pre>
 *
 * <p>Or directly: {@code GET /api/qr/COPY-001?size=300}
 */
@RestController
@RequestMapping("/api/qr")
public class QrCodeController {

    private final QrCodeService qrCodeService;

    public QrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    /**
     * Returns a QR code PNG image whose encoded content is {@code copyId}.
     *
     * @param copyId the BookCopy CopyID to encode in the QR
     * @param size   optional pixel size (default 250)
     */
    @GetMapping(value = "/{copyId}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(
            @PathVariable String copyId,
            @RequestParam(defaultValue = "250") int size) {

        byte[] png = qrCodeService.generatePng(copyId, size);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400") // cache 24 h
                .body(png);
    }
}
