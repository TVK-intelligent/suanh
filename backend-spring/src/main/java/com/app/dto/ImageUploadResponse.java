package com.app.dto;

import com.app.entity.ProcessingStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO trả về kết quả sau khi xử lý ảnh
 */
public class ImageUploadResponse {

    private Long id;
    private String originalFilename;
    private String originalUrl;
    private String processedUrl;
    private List<String> detectedObjects;
    private List<String> yoloLabels; // Nhãn từ YOLO detection
    private ProcessingStatus status;
    private Integer processingTimeMs;
    private Integer imageWidth;
    private Integer imageHeight;
    private LocalDateTime createdAt;
    private String errorMessage;
    private String imageCaption; // Mô tả ảnh từ Gemini Vision
    private Double psnr; // Peak Signal-to-Noise Ratio (dB)
    private Double ssim; // Structural Similarity Index (0-1)

    // Constructors
    public ImageUploadResponse() {
    }

    public static ImageUploadResponse success(Long id, String originalFilename,
            String originalUrl, String processedUrl, List<String> detectedObjects,
            Integer processingTimeMs, Integer width, Integer height) {
        ImageUploadResponse response = new ImageUploadResponse();
        response.id = id;
        response.originalFilename = originalFilename;
        response.originalUrl = originalUrl;
        response.processedUrl = processedUrl;
        response.detectedObjects = detectedObjects;
        response.status = ProcessingStatus.COMPLETED;
        response.processingTimeMs = processingTimeMs;
        response.imageWidth = width;
        response.imageHeight = height;
        response.createdAt = LocalDateTime.now();
        return response;
    }

    public static ImageUploadResponse processing(Long id, String originalFilename) {
        ImageUploadResponse response = new ImageUploadResponse();
        response.id = id;
        response.originalFilename = originalFilename;
        response.status = ProcessingStatus.PROCESSING;
        response.createdAt = LocalDateTime.now();
        return response;
    }

    public static ImageUploadResponse error(String errorMessage) {
        ImageUploadResponse response = new ImageUploadResponse();
        response.status = ProcessingStatus.FAILED;
        response.errorMessage = errorMessage;
        response.createdAt = LocalDateTime.now();
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getProcessedUrl() {
        return processedUrl;
    }

    public void setProcessedUrl(String processedUrl) {
        this.processedUrl = processedUrl;
    }

    public List<String> getDetectedObjects() {
        return detectedObjects;
    }

    public void setDetectedObjects(List<String> detectedObjects) {
        this.detectedObjects = detectedObjects;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
    }

    public Integer getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Integer processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getImageCaption() {
        return imageCaption;
    }

    public void setImageCaption(String imageCaption) {
        this.imageCaption = imageCaption;
    }

    public List<String> getYoloLabels() {
        return yoloLabels;
    }

    public void setYoloLabels(List<String> yoloLabels) {
        this.yoloLabels = yoloLabels;
    }

    public Double getPsnr() {
        return psnr;
    }

    public void setPsnr(Double psnr) {
        this.psnr = psnr;
    }

    public Double getSsim() {
        return ssim;
    }

    public void setSsim(Double ssim) {
        this.ssim = ssim;
    }
}
