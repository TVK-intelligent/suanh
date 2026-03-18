package com.app.service;

import com.app.dto.ImageUploadResponse;
import com.app.entity.ImageTask;
import com.app.entity.ProcessingStatus;
import com.app.repository.ImageTaskRepository;
import com.app.util.ImageUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.awt.image.BufferedImage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service chính xử lý ảnh
 * Kết hợp Denoise và Object Detection
 */
@Service
public class ImageProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingService.class);

    private final DenoiseService denoiseService;
    private final YoloDetectionService yoloDetectionService;
    private final GeminiVisionService geminiVisionService;
    private final ImageTaskRepository imageTaskRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.original-dir}")
    private String originalDir;

    @Value("${app.upload.processed-dir}")
    private String processedDir;

    public ImageProcessingService(DenoiseService denoiseService,
            YoloDetectionService yoloDetectionService,
            GeminiVisionService geminiVisionService,
            ImageTaskRepository imageTaskRepository,
            ObjectMapper objectMapper) {
        this.denoiseService = denoiseService;
        this.yoloDetectionService = yoloDetectionService;
        this.geminiVisionService = geminiVisionService;
        this.imageTaskRepository = imageTaskRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            // Tạo thư mục upload nếu chưa tồn tại
            Files.createDirectories(Paths.get(originalDir));
            Files.createDirectories(Paths.get(processedDir));
            logger.info("✓ Đã tạo thư mục upload: {} và {}", originalDir, processedDir);
        } catch (IOException e) {
            logger.error("Không thể tạo thư mục upload", e);
        }
    }

    /**
     * Xử lý ảnh: Lưu file, khử nhiễu và nhận diện đối tượng
     */
    public ImageUploadResponse processImage(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        ImageTask task = null;

        try {
            // 0. Kiểm tra null cho file
            if (file == null) {
                logger.error("File upload là null");
                return ImageUploadResponse.error("Không nhận được file. Vui lòng thử lại.");
            }

            // Kiểm tra file content
            byte[] fileBytes = file.getBytes();
            if (fileBytes == null || fileBytes.length == 0) {
                logger.error("File content là null hoặc rỗng");
                return ImageUploadResponse.error("File rỗng hoặc không đọc được. Vui lòng chọn file khác.");
            }

            // 1. Validate file
            validateFile(file);

            // 2. Lưu file gốc
            String originalFilename = file.getOriginalFilename();
            String uniqueFilename = generateUniqueFilename(originalFilename);
            Path originalPath = Paths.get(originalDir, uniqueFilename);
            Files.write(originalPath, fileBytes);

            logger.info("Đã lưu file gốc: {}", originalPath);

            // 3. Tạo task record
            task = new ImageTask(originalFilename, originalPath.toString());
            task.setProcessingStatus(ProcessingStatus.PROCESSING);
            task.setFileSize(file.getSize());

            // Lấy kích thước ảnh
            int[] dimensions = ImageUtils.getImageDimensions(fileBytes);
            task.setImageWidth(dimensions[0]);
            task.setImageHeight(dimensions[1]);

            task = imageTaskRepository.save(task);

            // 4. Khử nhiễu ảnh
            byte[] processedImageData = denoiseService.denoiseImage(fileBytes);

            // 5. Lưu ảnh đã xử lý
            String processedFilename = "processed_" + uniqueFilename;
            Path processedPath = Paths.get(processedDir, processedFilename);
            Files.write(processedPath, processedImageData);

            logger.info("Đã lưu ảnh đã xử lý: {}", processedPath);

            // 6. Nhận diện bằng YOLO (local)
            List<String> yoloLabels = yoloDetectionService.detectObjects(fileBytes);

            // 7. Phân tích ảnh bằng Gemini Vision (mô tả + bổ sung nhãn)
            GeminiVisionService.ImageAnalysisResult analysisResult = geminiVisionService.analyzeImage(fileBytes);

            List<String> labels = mergeLabels(yoloLabels, analysisResult.getDetectedObjects());
            String imageCaption = analysisResult.getDescription();

            // 8. Cập nhật task
            task.setProcessedPath(processedPath.toString());
            task.setDetectedObjects(objectMapper.writeValueAsString(labels));
            task.setImageCaption(imageCaption);
            task.setProcessingStatus(ProcessingStatus.COMPLETED);
            task.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));

            task = imageTaskRepository.save(task);

            logger.info("✓ Hoàn thành xử lý ảnh ID={} trong {}ms", task.getId(), task.getProcessingTimeMs());

            // 9. Tạo response
            ImageUploadResponse response = ImageUploadResponse.success(
                    task.getId(),
                    originalFilename,
                    "/uploads/original/" + uniqueFilename,
                    "/uploads/processed/" + processedFilename,
                    labels,
                    task.getProcessingTimeMs(),
                    task.getImageWidth(),
                    task.getImageHeight());
            response.setImageCaption(imageCaption);
            return response;

        } catch (Exception e) {
            logger.error("Lỗi xử lý ảnh: {}", e.getMessage(), e);

            if (task != null) {
                task.setProcessingStatus(ProcessingStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                imageTaskRepository.save(task);
            }

            return ImageUploadResponse.error(e.getMessage());
        }
    }

    /**
     * Chỉ khử nhiễu ảnh (không nhận diện)
     */
    public ImageUploadResponse denoiseOnly(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        ImageTask task = null;

        try {
            if (file == null || file.isEmpty()) {
                return ImageUploadResponse.error("Không nhận được file. Vui lòng thử lại.");
            }

            byte[] fileBytes = file.getBytes();
            if (fileBytes == null || fileBytes.length == 0) {
                return ImageUploadResponse.error("File rỗng hoặc không đọc được.");
            }

            validateFile(file);

            String originalFilename = file.getOriginalFilename();
            String uniqueFilename = generateUniqueFilename(originalFilename);
            Path originalPath = Paths.get(originalDir, uniqueFilename);
            Files.write(originalPath, fileBytes);

            task = new ImageTask(originalFilename, originalPath.toString());
            task.setProcessingStatus(ProcessingStatus.PROCESSING);
            task.setFileSize(file.getSize());

            int[] dimensions = ImageUtils.getImageDimensions(fileBytes);
            task.setImageWidth(dimensions[0]);
            task.setImageHeight(dimensions[1]);
            task = imageTaskRepository.save(task);

            // Chỉ khử nhiễu
            byte[] processedImageData = denoiseService.denoiseImage(fileBytes);

            String processedFilename = "processed_" + uniqueFilename;
            Path processedPath = Paths.get(processedDir, processedFilename);
            Files.write(processedPath, processedImageData);

            // Tính PSNR/SSIM
            Double psnrValue = null;
            Double ssimValue = null;
            try {
                BufferedImage originalImage = ImageUtils.bytesToImage(fileBytes);
                BufferedImage processedImage = ImageUtils.bytesToImage(processedImageData);
                ImageUtils.QualityMetrics metrics = ImageUtils.calculateMetrics(originalImage, processedImage);
                psnrValue = Math.round(metrics.getPsnr() * 100.0) / 100.0;
                ssimValue = Math.round(metrics.getSsim() * 10000.0) / 10000.0;
                logger.info("📊 Chỉ số chất lượng - PSNR: {} dB, SSIM: {}", psnrValue, ssimValue);
            } catch (Exception metricsEx) {
                logger.warn("Không thể tính PSNR/SSIM: {}", metricsEx.getMessage());
            }

            task.setProcessedPath(processedPath.toString());
            task.setProcessingStatus(ProcessingStatus.COMPLETED);
            task.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));
            task = imageTaskRepository.save(task);

            logger.info("✓ Hoàn thành khử nhiễu ảnh ID={} trong {}ms", task.getId(), task.getProcessingTimeMs());

            ImageUploadResponse response = ImageUploadResponse.success(
                    task.getId(), originalFilename,
                    "/uploads/original/" + uniqueFilename,
                    "/uploads/processed/" + processedFilename,
                    null, task.getProcessingTimeMs(),
                    task.getImageWidth(), task.getImageHeight());
            response.setPsnr(psnrValue);
            response.setSsim(ssimValue);
            return response;

        } catch (Exception e) {
            logger.error("Lỗi khử nhiễu ảnh: {}", e.getMessage(), e);
            if (task != null) {
                task.setProcessingStatus(ProcessingStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                imageTaskRepository.save(task);
            }
            return ImageUploadResponse.error(e.getMessage());
        }
    }

    /**
     * Chỉ nhận diện đối tượng bằng Gemini Vision (không khử nhiễu)
     */
    public ImageUploadResponse detectOnly(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        ImageTask task = null;

        try {
            if (file == null || file.isEmpty()) {
                return ImageUploadResponse.error("Không nhận được file. Vui lòng thử lại.");
            }

            byte[] fileBytes = file.getBytes();
            if (fileBytes == null || fileBytes.length == 0) {
                return ImageUploadResponse.error("File rỗng hoặc không đọc được.");
            }

            validateFile(file);

            String originalFilename = file.getOriginalFilename();
            String uniqueFilename = generateUniqueFilename(originalFilename);
            Path originalPath = Paths.get(originalDir, uniqueFilename);
            Files.write(originalPath, fileBytes);

            task = new ImageTask(originalFilename, originalPath.toString());
            task.setProcessingStatus(ProcessingStatus.PROCESSING);
            task.setFileSize(file.getSize());

            int[] dimensions = ImageUtils.getImageDimensions(fileBytes);
            task.setImageWidth(dimensions[0]);
            task.setImageHeight(dimensions[1]);
            task = imageTaskRepository.save(task);

            // Nhận diện YOLO (local)
            List<String> yoloLabels = yoloDetectionService.detectObjects(fileBytes);

            // Gemini dùng để mô tả + bổ sung nhãn
            GeminiVisionService.ImageAnalysisResult analysisResult = geminiVisionService.analyzeImage(fileBytes);
            List<String> labels = mergeLabels(yoloLabels, analysisResult.getDetectedObjects());
            String imageCaption = analysisResult.getDescription();

            task.setDetectedObjects(objectMapper.writeValueAsString(labels));
            task.setImageCaption(imageCaption);
            task.setProcessingStatus(ProcessingStatus.COMPLETED);
            task.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));
            task = imageTaskRepository.save(task);

            logger.info("✓ Hoàn thành nhận diện ảnh ID={} trong {}ms", task.getId(), task.getProcessingTimeMs());

            ImageUploadResponse response = ImageUploadResponse.success(
                    task.getId(), originalFilename,
                    "/uploads/original/" + uniqueFilename,
                    null, labels, task.getProcessingTimeMs(),
                    task.getImageWidth(), task.getImageHeight());
            response.setImageCaption(imageCaption);
            return response;

        } catch (Exception e) {
            logger.error("Lỗi nhận diện ảnh: {}", e.getMessage(), e);
            if (task != null) {
                task.setProcessingStatus(ProcessingStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                imageTaskRepository.save(task);
            }
            return ImageUploadResponse.error(e.getMessage());
        }
    }

    /**
     * Xử lý ảnh bất đồng bộ
     */
    @Async
    public CompletableFuture<ImageUploadResponse> processImageAsync(MultipartFile file) {
        return CompletableFuture.completedFuture(processImage(file));
    }

    /**
     * Lấy thống kê tổng quát
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();

        long totalImages = imageTaskRepository.count();
        long completedCount = imageTaskRepository.countByProcessingStatus(ProcessingStatus.COMPLETED);
        long failedCount = imageTaskRepository.countByProcessingStatus(ProcessingStatus.FAILED);
        long processingCount = imageTaskRepository.countByProcessingStatus(ProcessingStatus.PROCESSING);
        long totalFileSize = imageTaskRepository.sumTotalFileSize();
        double avgTime = imageTaskRepository.avgProcessingTime();
        long todayCount = imageTaskRepository.countTodayTasks(
                java.time.LocalDate.now().atStartOfDay());

        double successRate = totalImages > 0
                ? Math.round((completedCount * 100.0 / totalImages) * 10) / 10.0
                : 0;

        stats.put("totalImages", totalImages);
        stats.put("completedCount", completedCount);
        stats.put("failedCount", failedCount);
        stats.put("processingCount", processingCount);
        stats.put("todayCount", todayCount);
        stats.put("successRate", successRate);
        stats.put("avgProcessingTimeMs", Math.round(avgTime));
        stats.put("totalFileSizeMB", Math.round(totalFileSize / (1024.0 * 1024) * 100) / 100.0);

        return stats;
    }

    /**
     * Tìm kiếm lịch sử theo tên file
     */
    public Page<ImageTask> searchHistory(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return imageTaskRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return imageTaskRepository.searchByFilename(keyword.trim(), pageable);
    }

    /**
     * Chỉ khử nhiễu ảnh với cường độ tuỳ chỉnh
     */
    public ImageUploadResponse denoiseWithStrength(MultipartFile file, int strength) {
        long startTime = System.currentTimeMillis();
        ImageTask task = null;

        try {
            if (file == null || file.isEmpty()) {
                return ImageUploadResponse.error("Không nhận được file. Vui lòng thử lại.");
            }

            byte[] fileBytes = file.getBytes();
            if (fileBytes == null || fileBytes.length == 0) {
                return ImageUploadResponse.error("File rỗng hoặc không đọc được.");
            }

            validateFile(file);

            String originalFilename = file.getOriginalFilename();
            String uniqueFilename = generateUniqueFilename(originalFilename);
            Path originalPath = Paths.get(originalDir, uniqueFilename);
            Files.write(originalPath, fileBytes);

            task = new ImageTask(originalFilename, originalPath.toString());
            task.setProcessingStatus(ProcessingStatus.PROCESSING);
            task.setFileSize(file.getSize());

            int[] dimensions = ImageUtils.getImageDimensions(fileBytes);
            task.setImageWidth(dimensions[0]);
            task.setImageHeight(dimensions[1]);
            task = imageTaskRepository.save(task);

            // Khử nhiễu với cường độ tuỳ chỉnh
            byte[] processedImageData = denoiseService.denoiseImage(fileBytes, strength);

            String processedFilename = "processed_" + uniqueFilename;
            Path processedPath = Paths.get(processedDir, processedFilename);
            Files.write(processedPath, processedImageData);

            // Tính PSNR/SSIM
            Double psnrValue = null;
            Double ssimValue = null;
            try {
                BufferedImage originalImage = ImageUtils.bytesToImage(fileBytes);
                BufferedImage processedImage = ImageUtils.bytesToImage(processedImageData);
                ImageUtils.QualityMetrics metrics = ImageUtils.calculateMetrics(originalImage, processedImage);
                psnrValue = Math.round(metrics.getPsnr() * 100.0) / 100.0;
                ssimValue = Math.round(metrics.getSsim() * 10000.0) / 10000.0;
                logger.info("📊 Chỉ số chất lượng - PSNR: {} dB, SSIM: {}", psnrValue, ssimValue);
            } catch (Exception metricsEx) {
                logger.warn("Không thể tính PSNR/SSIM: {}", metricsEx.getMessage());
            }

            task.setProcessedPath(processedPath.toString());
            task.setProcessingStatus(ProcessingStatus.COMPLETED);
            task.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));
            task = imageTaskRepository.save(task);

            logger.info("✓ Hoàn thành khử nhiễu (strength={}) ảnh ID={} trong {}ms",
                    strength, task.getId(), task.getProcessingTimeMs());

            ImageUploadResponse response = ImageUploadResponse.success(
                    task.getId(), originalFilename,
                    "/uploads/original/" + uniqueFilename,
                    "/uploads/processed/" + processedFilename,
                    null, task.getProcessingTimeMs(),
                    task.getImageWidth(), task.getImageHeight());
            response.setPsnr(psnrValue);
            response.setSsim(ssimValue);
            return response;

        } catch (Exception e) {
            logger.error("Lỗi khử nhiễu ảnh: {}", e.getMessage(), e);
            if (task != null) {
                task.setProcessingStatus(ProcessingStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                imageTaskRepository.save(task);
            }
            return ImageUploadResponse.error(e.getMessage());
        }
    }

    /**
     * Validate file upload
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPEG, PNG, etc.)");
        }

        // Kiểm tra định dạng hỗ trợ
        String[] supportedTypes = { "image/jpeg", "image/png", "image/webp", "image/bmp" };
        boolean supported = false;
        for (String type : supportedTypes) {
            if (type.equals(contentType)) {
                supported = true;
                break;
            }
        }

        if (!supported) {
            throw new IllegalArgumentException("Định dạng không hỗ trợ. Hỗ trợ: JPEG, PNG, WebP, BMP");
        }

        // Kiểm tra kích thước (max 50MB đã cấu hình trong properties)
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("File quá lớn. Tối đa 50MB");
        }
    }

    /**
     * Tạo tên file unique
     */
    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Lấy thông tin task theo ID
     */
    public ImageTask getTaskById(Long id) {
        return imageTaskRepository.findById(id).orElse(null);
    }

    /**
     * Lấy lịch sử xử lý với phân trang
     */
    public Page<ImageTask> getHistory(Pageable pageable) {
        return imageTaskRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Chuyển đổi ImageTask thành ImageUploadResponse
     */
    public ImageUploadResponse toResponse(ImageTask task) {
        try {
            List<String> labels = new java.util.ArrayList<>();

            // Kiểm tra null/empty trước khi parse JSON
            String detectedObjects = task.getDetectedObjects();
            if (detectedObjects != null && !detectedObjects.isEmpty() && !detectedObjects.equals("null")) {
                labels = objectMapper.readValue(
                        detectedObjects,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            }

            // Trích xuất filename từ path
            String originalUrl = task.getOriginalPath() != null
                    ? "/uploads/original/" + Paths.get(task.getOriginalPath()).getFileName()
                    : null;
            String processedUrl = task.getProcessedPath() != null
                    ? "/uploads/processed/" + Paths.get(task.getProcessedPath()).getFileName()
                    : null;

            ImageUploadResponse response = new ImageUploadResponse();
            response.setId(task.getId());
            response.setOriginalFilename(task.getOriginalFilename());
            response.setOriginalUrl(originalUrl);
            response.setProcessedUrl(processedUrl);
            response.setDetectedObjects(labels);
            response.setStatus(task.getProcessingStatus());
            response.setProcessingTimeMs(task.getProcessingTimeMs());
            response.setImageWidth(task.getImageWidth());
            response.setImageHeight(task.getImageHeight());
            response.setCreatedAt(task.getCreatedAt());
            response.setErrorMessage(task.getErrorMessage());
            response.setImageCaption(task.getImageCaption());

            return response;
        } catch (JsonProcessingException e) {
            logger.error("Lỗi parse JSON detected objects", e);
            return ImageUploadResponse.error("Lỗi đọc dữ liệu");
        }
    }

    /**
     * Xóa task theo ID và các file ảnh liên quan
     */
    public boolean deleteTask(Long id) {
        return imageTaskRepository.findById(id).map(task -> {
            deleteTaskFiles(task);
            imageTaskRepository.delete(task);
            logger.info("✓ Đã xóa task ID={}", id);
            return true;
        }).orElse(false);
    }

    /**
     * Xóa toàn bộ task và file ảnh
     */
    public int deleteAllTasks() {
        List<ImageTask> allTasks = imageTaskRepository.findAll();
        int count = allTasks.size();

        for (ImageTask task : allTasks) {
            deleteTaskFiles(task);
        }

        imageTaskRepository.deleteAll();
        logger.info("✓ Đã xóa toàn bộ {} task", count);
        return count;
    }

    /**
     * Xóa file ảnh trên đĩa
     */
    private void deleteTaskFiles(ImageTask task) {
        try {
            if (task.getOriginalPath() != null) {
                Files.deleteIfExists(Paths.get(task.getOriginalPath()));
            }
            if (task.getProcessedPath() != null) {
                Files.deleteIfExists(Paths.get(task.getProcessedPath()));
            }
        } catch (IOException e) {
            logger.warn("Không thể xóa file của task {}: {}", task.getId(), e.getMessage());
        }
    }

    /**
     * Gộp nhãn từ nhiều nguồn, loại bỏ trùng lặp và giữ thứ tự.
     */
    private List<String> mergeLabels(List<String>... labelGroups) {
        Set<String> merged = new LinkedHashSet<>();
        for (List<String> group : labelGroups) {
            if (group == null) {
                continue;
            }
            for (String label : group) {
                if (label != null && !label.isBlank()) {
                    merged.add(label.trim());
                }
            }
        }
        return new java.util.ArrayList<>(merged);
    }
}
