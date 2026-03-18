package com.app.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility class cho xử lý ảnh
 * Hỗ trợ chuyển đổi format, đo lường chất lượng (PSNR/SSIM)
 */
public class ImageUtils {

    /**
     * Chuyển đổi byte array thành BufferedImage
     */
    public static BufferedImage bytesToImage(byte[] imageData) throws IOException {
        if (imageData == null || imageData.length == 0) {
            throw new IOException("Dữ liệu ảnh là null hoặc rỗng");
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        BufferedImage image = ImageIO.read(bais);
        if (image == null) {
            throw new IOException("Không thể đọc ảnh từ dữ liệu. Định dạng có thể không được hỗ trợ.");
        }
        return image;
    }

    /**
     * Chuyển đổi BufferedImage thành byte array (PNG format)
     */
    public static byte[] imageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    /**
     * Resize ảnh về kích thước mong muốn
     */
    public static BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resized;
    }

    /**
     * Lấy thông tin kích thước ảnh
     */
    public static int[] getImageDimensions(byte[] imageData) throws IOException {
        BufferedImage image = bytesToImage(imageData);
        return new int[] { image.getWidth(), image.getHeight() };
    }

    /**
     * Kết quả đánh giá chất lượng ảnh
     */
    public static class QualityMetrics {
        private final double psnr;
        private final double ssim;

        public QualityMetrics(double psnr, double ssim) {
            this.psnr = psnr;
            this.ssim = ssim;
        }

        public double getPsnr() {
            return psnr;
        }

        public double getSsim() {
            return ssim;
        }
    }

    /**
     * Tính PSNR và SSIM giữa ảnh gốc và ảnh đã xử lý
     * Ảnh xử lý sẽ được resize về cùng kích thước ảnh gốc để so sánh
     */
    public static QualityMetrics calculateMetrics(BufferedImage original, BufferedImage processed) {
        // Resize processed về cùng kích thước original để so sánh công bằng
        int w = original.getWidth();
        int h = original.getHeight();
        BufferedImage resizedProcessed = resizeImage(processed, w, h);

        double psnr = calculatePSNR(original, resizedProcessed);
        double ssim = calculateSSIM(original, resizedProcessed);
        return new QualityMetrics(psnr, ssim);
    }

    /**
     * PSNR (Peak Signal-to-Noise Ratio)
     * Đo mức độ sai lệch pixel giữa 2 ảnh
     * Giá trị cao hơn = ít nhiễu hơn (thường 25-50 dB)
     */
    public static double calculatePSNR(BufferedImage img1, BufferedImage img2) {
        int width = img1.getWidth();
        int height = img1.getHeight();

        double mse = 0;
        int totalPixels = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);

                int r1 = (rgb1 >> 16) & 0xFF, g1 = (rgb1 >> 8) & 0xFF, b1 = rgb1 & 0xFF;
                int r2 = (rgb2 >> 16) & 0xFF, g2 = (rgb2 >> 8) & 0xFF, b2 = rgb2 & 0xFF;

                mse += Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2);
            }
        }

        mse /= (totalPixels * 3.0); // 3 channels RGB

        if (mse == 0)
            return Double.POSITIVE_INFINITY; // Hai ảnh giống hệt

        return 10 * Math.log10((255.0 * 255.0) / mse);
    }

    /**
     * SSIM (Structural Similarity Index Measure)
     * Đo mức độ tương đồng cấu trúc giữa 2 ảnh
     * Giá trị từ 0 đến 1 (1 = giống hệt)
     * 
     * Tính SSIM trên cửa sổ 8x8 và lấy trung bình
     */
    public static double calculateSSIM(BufferedImage img1, BufferedImage img2) {
        int width = img1.getWidth();
        int height = img1.getHeight();
        int windowSize = 8;

        // Hằng số ổn định (tránh chia cho 0)
        double C1 = Math.pow(0.01 * 255, 2);
        double C2 = Math.pow(0.03 * 255, 2);

        double totalSSIM = 0;
        int windowCount = 0;

        // Duyệt qua từng cửa sổ
        for (int y = 0; y <= height - windowSize; y += windowSize) {
            for (int x = 0; x <= width - windowSize; x += windowSize) {

                double mean1 = 0, mean2 = 0;
                int n = windowSize * windowSize;

                // Tính mean
                for (int wy = 0; wy < windowSize; wy++) {
                    for (int wx = 0; wx < windowSize; wx++) {
                        mean1 += getLuminance(img1.getRGB(x + wx, y + wy));
                        mean2 += getLuminance(img2.getRGB(x + wx, y + wy));
                    }
                }
                mean1 /= n;
                mean2 /= n;

                // Tính variance và covariance
                double var1 = 0, var2 = 0, covar = 0;
                for (int wy = 0; wy < windowSize; wy++) {
                    for (int wx = 0; wx < windowSize; wx++) {
                        double l1 = getLuminance(img1.getRGB(x + wx, y + wy));
                        double l2 = getLuminance(img2.getRGB(x + wx, y + wy));
                        var1 += Math.pow(l1 - mean1, 2);
                        var2 += Math.pow(l2 - mean2, 2);
                        covar += (l1 - mean1) * (l2 - mean2);
                    }
                }
                var1 /= (n - 1);
                var2 /= (n - 1);
                covar /= (n - 1);

                // Công thức SSIM
                double numerator = (2 * mean1 * mean2 + C1) * (2 * covar + C2);
                double denominator = (mean1 * mean1 + mean2 * mean2 + C1) * (var1 + var2 + C2);

                totalSSIM += numerator / denominator;
                windowCount++;
            }
        }

        return windowCount > 0 ? totalSSIM / windowCount : 0;
    }

    /**
     * Chuyển pixel RGB sang luminance (độ sáng)
     * Công thức chuẩn ITU-R BT.601
     */
    private static double getLuminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0.299 * r + 0.587 * g + 0.114 * b;
    }
}
