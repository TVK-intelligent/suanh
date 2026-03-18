package com.app.exception;

import com.app.dto.ImageUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global Exception Handler
 * Xử lý các exception chung cho toàn bộ ứng dụng
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ImageUploadResponse> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("Validation error: {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ImageUploadResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ImageUploadResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        logger.warn("File quá lớn: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ImageUploadResponse.error("File quá lớn. Tối đa 50MB"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ImageUploadResponse> handleGeneral(Exception e) {
        logger.error("Lỗi hệ thống: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ImageUploadResponse.error("Lỗi hệ thống: " + e.getMessage()));
    }
}
