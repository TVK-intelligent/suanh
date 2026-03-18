package com.app.controller;

import com.app.dto.ImageUploadResponse;
import com.app.entity.ImageTask;
import com.app.service.ImageProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller xử lý các API liên quan đến ảnh
 */
@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    private final ImageProcessingService imageProcessingService;

    public ImageController(ImageProcessingService imageProcessingService) {
        this.imageProcessingService = imageProcessingService;
    }

    /**
     * Upload và xử lý ảnh
     * POST /api/images/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestParam(value = "file", required = false) MultipartFile file) {

        // Kiểm tra file null trước khi xử lý
        if (file == null || file.isEmpty()) {
            logger.warn("Nhận request upload nhưng không có file hoặc file rỗng");
            return ResponseEntity.badRequest()
                    .body(ImageUploadResponse.error("Vui lòng chọn file ảnh để tải lên"));
        }

        logger.info("Nhận request upload ảnh: {} ({}KB)",
                file.getOriginalFilename(),
                file.getSize() / 1024);

        ImageUploadResponse response = imageProcessingService.processImage(file);

        // Luôn trả về 200 OK, client sẽ check status field
        return ResponseEntity.ok(response);
    }

    /**
     * Chỉ khử nhiễu ảnh (hỗ trợ tuỳ chỉnh cường độ)
     * POST /api/images/denoise?strength=10
     */
    @PostMapping("/denoise")
    public ResponseEntity<ImageUploadResponse> denoiseImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "strength", required = false, defaultValue = "0") int strength) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ImageUploadResponse.error("Vui lòng chọn file ảnh để tải lên"));
        }

        logger.info("Nhận request khử nhiễu ảnh: {} ({}KB), strength={}",
                file.getOriginalFilename(), file.getSize() / 1024, strength);

        ImageUploadResponse response;
        if (strength > 0) {
            response = imageProcessingService.denoiseWithStrength(file, strength);
        } else {
            response = imageProcessingService.denoiseOnly(file);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Chỉ nhận diện đối tượng bằng Gemini Vision
     * POST /api/images/detect
     */
    @PostMapping("/detect")
    public ResponseEntity<ImageUploadResponse> detectImage(
            @RequestParam(value = "file", required = false) MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ImageUploadResponse.error("Vui lòng chọn file ảnh để tải lên"));
        }

        logger.info("Nhận request nhận diện ảnh: {} ({}KB)",
                file.getOriginalFilename(), file.getSize() / 1024);

        ImageUploadResponse response = imageProcessingService.detectOnly(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thông tin một task theo ID
     * GET /api/images/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ImageUploadResponse> getImage(@PathVariable Long id) {
        ImageTask task = imageProcessingService.getTaskById(id);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(imageProcessingService.toResponse(task));
    }

    /**
     * Lấy lịch sử xử lý ảnh với phân trang và tìm kiếm
     * GET /api/images/history?page=0&size=10&keyword=abc
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ImageTask> tasksPage = imageProcessingService.searchHistory(keyword, pageable);

        List<ImageUploadResponse> responses = tasksPage.getContent().stream()
                .map(imageProcessingService::toResponse)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("images", responses);
        result.put("currentPage", tasksPage.getNumber());
        result.put("totalItems", tasksPage.getTotalElements());
        result.put("totalPages", tasksPage.getTotalPages());

        return ResponseEntity.ok(result);
    }

    /**
     * Thống kê tổng quát
     * GET /api/images/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(imageProcessingService.getStatistics());
    }

    /**
     * Health check endpoint
     * GET /api/images/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AI Image Processor");
        health.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(health);
    }

    /**
     * Xóa một task theo ID (bao gồm file ảnh)
     * DELETE /api/images/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable Long id) {
        boolean deleted = imageProcessingService.deleteTask(id);

        Map<String, Object> result = new HashMap<>();
        if (deleted) {
            result.put("message", "Đã xóa thành công");
            return ResponseEntity.ok(result);
        } else {
            result.put("message", "Không tìm thấy ảnh");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Xóa toàn bộ lịch sử (bao gồm file ảnh)
     * DELETE /api/images/history
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Object>> clearHistory() {
        int count = imageProcessingService.deleteAllTasks();

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Đã xóa " + count + " ảnh");
        result.put("deletedCount", count);
        return ResponseEntity.ok(result);
    }
}
