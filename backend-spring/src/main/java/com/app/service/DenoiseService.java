package com.app.service;

import jakarta.annotation.PostConstruct;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.photo.Photo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service khử nhiễu ảnh sử dụng OpenCV
 * Thuật toán: Non-Local Means Denoising (fastNlMeansDenoisingColored)
 * - Không cần model AI/ONNX
 * - Hiệu quả với nhiễu Gaussian (ảnh chụp thiếu sáng, ISO cao)
 */
@Service
public class DenoiseService {

    private static final Logger logger = LoggerFactory.getLogger(DenoiseService.class);

    @Value("${app.denoise.strength:10}")
    private int denoiseStrength;

    private boolean opencvLoaded = false;

    @PostConstruct
    public void init() {
        try {
            logger.info("Đang khởi tạo OpenCV cho Denoise Service...");
            nu.pattern.OpenCV.loadLocally();
            opencvLoaded = true;
            logger.info("✓ Đã tải OpenCV {} thành công!", Core.VERSION);
            logger.info("  Cường độ khử nhiễu mặc định: h={}", denoiseStrength);
        } catch (Exception e) {
            logger.error("✗ Lỗi khi tải OpenCV: " + e.getMessage(), e);
            logger.warn("⚠ Chế độ demo sẽ được sử dụng (trả về ảnh gốc).");
        }
    }

    /**
     * Khử nhiễu ảnh với cường độ mặc định
     *
     * @param imageData byte array của ảnh gốc
     * @return byte array của ảnh đã khử nhiễu (PNG format)
     */
    public byte[] denoiseImage(byte[] imageData) {
        return denoiseImage(imageData, denoiseStrength);
    }

    /**
     * Khử nhiễu ảnh với cường độ tuỳ chỉnh
     *
     * @param imageData byte array của ảnh gốc
     * @param strength  cường độ khử nhiễu (1-30, giá trị cao = mượt hơn nhưng mất
     *                  chi tiết)
     * @return byte array của ảnh đã khử nhiễu (PNG format)
     */
    public byte[] denoiseImage(byte[] imageData, int strength) {
        if (!opencvLoaded) {
            logger.info("OpenCV chưa được tải, sử dụng chế độ demo (trả về ảnh gốc)");
            return imageData;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 1. Decode ảnh từ byte array sang Mat (OpenCV matrix)
            Mat src = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
            if (src.empty()) {
                logger.error("Không thể decode ảnh từ byte array");
                return imageData;
            }

            logger.info("Đang khử nhiễu ảnh {}x{} với h={}", src.cols(), src.rows(), strength);

            // 2. Áp dụng Non-Local Means Denoising cho ảnh màu
            Mat dst = new Mat();
            Photo.fastNlMeansDenoisingColored(
                    src, // Ảnh đầu vào
                    dst, // Ảnh đầu ra
                    strength, // h: cường độ lọc luminance
                    strength, // hForColorComponents: cường độ lọc màu
                    7, // templateWindowSize (nên là số lẻ, mặc định 7)
                    21 // searchWindowSize (nên là số lẻ, mặc định 21)
            );

            // 3. Encode ảnh kết quả sang PNG byte array
            MatOfByte resultBuffer = new MatOfByte();
            Imgcodecs.imencode(".png", dst, resultBuffer);
            byte[] result = resultBuffer.toArray();

            // 4. Giải phóng bộ nhớ OpenCV
            src.release();
            dst.release();
            resultBuffer.release();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("✓ Hoàn thành khử nhiễu trong {}ms. Output: {} bytes", duration, result.length);

            return result;

        } catch (Exception e) {
            logger.error("Lỗi khi khử nhiễu ảnh: {}", e.getMessage(), e);
            return imageData; // Trả về ảnh gốc nếu lỗi
        }
    }

    /**
     * Kiểm tra OpenCV đã được tải chưa
     */
    public boolean isModelLoaded() {
        return opencvLoaded;
    }
}
