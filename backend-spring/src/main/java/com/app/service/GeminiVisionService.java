package com.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service phân tích ảnh sử dụng Google Gemini Vision API
 * Thay thế hoàn toàn YOLOv5/ONNX - đơn giản và mạnh mẽ hơn
 */
@Service
public class GeminiVisionService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiVisionService.class);

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    /**
     * Kết quả phân tích ảnh từ Gemini
     */
    public static class ImageAnalysisResult {
        private String description;
        private List<String> detectedObjects;

        public ImageAnalysisResult(String description, List<String> detectedObjects) {
            this.description = description;
            this.detectedObjects = detectedObjects;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getDetectedObjects() {
            return detectedObjects;
        }
    }

    /**
     * Phân tích ảnh: nhận diện đối tượng + mô tả chi tiết
     */
    public ImageAnalysisResult analyzeImage(byte[] imageData) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            logger.warn("⚠ Gemini Vision chưa được cấu hình. Vui lòng thêm API key.");
            return new ImageAnalysisResult(
                    "Chưa cấu hình Gemini API. Vui lòng thêm API key trong application.properties",
                    Collections.emptyList());
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageData);

            // Prompt yêu cầu Gemini trả về JSON với cả objects và description
            String prompt = """
                    Phân tích bức ảnh này và trả về kết quả theo định dạng JSON sau:
                    {
                        "objects": ["danh sách các đối tượng nhìn thấy trong ảnh, mỗi đối tượng là 1-2 từ"],
                        "description": "một đoạn văn ngắn mô tả chi tiết bức ảnh (4-6 câu) bằng tiếng Việt. Hãy mô tả: (1) tổng quan cảnh/bối cảnh, (2) các đối tượng chính và vị trí của chúng, (3) màu sắc và ánh sáng, (4) không khí hoặc cảm xúc tổng thể"
                    }
                    Chỉ trả về JSON, không có text khác.
                    """;

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", prompt),
                                            Map.of(
                                                    "inlineData", Map.of(
                                                            "mimeType", "image/jpeg",
                                                            "data", base64Image))))),
                    "generationConfig", Map.of(
                            "temperature", 0.3,
                            "maxOutputTokens", 1024));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = GEMINI_API_URL + "?key=" + apiKey;
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String jsonResponse = extractTextFromResponse(response.getBody());
                return parseJsonResponse(jsonResponse);
            }

            return new ImageAnalysisResult("Không thể phân tích ảnh", Collections.emptyList());

        } catch (Exception e) {
            logger.error("Lỗi khi gọi Gemini Vision API: {}", e.getMessage());
            return new ImageAnalysisResult("Lỗi: " + e.getMessage(), Collections.emptyList());
        }
    }

    /**
     * Trích xuất text từ response của Gemini
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> body) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi parse response: {}", e.getMessage());
        }
        return "{}";
    }

    /**
     * Parse JSON response thành ImageAnalysisResult
     */
    @SuppressWarnings("unchecked")
    private ImageAnalysisResult parseJsonResponse(String jsonText) {
        try {
            jsonText = jsonText.trim();
            int startIndex = jsonText.indexOf('{');
            int endIndex = jsonText.lastIndexOf('}');
            if (startIndex >= 0 && endIndex > startIndex) {
                jsonText = jsonText.substring(startIndex, endIndex + 1);
            }

            // Parse JSON đơn giản
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> result = mapper.readValue(jsonText, Map.class);

            String description = (String) result.getOrDefault("description", "Không có mô tả");
            List<String> objects = (List<String>) result.getOrDefault("objects", Collections.emptyList());

            logger.info("✓ Phân tích ảnh thành công: {} đối tượng", objects.size());
            return new ImageAnalysisResult(description, objects);

        } catch (Exception e) {
            logger.warn("Không thể parse JSON, sử dụng text gốc: {}", e.getMessage());
            // Fallback: trả về text gốc làm description
            return new ImageAnalysisResult(jsonText, Collections.emptyList());
        }
    }

    /**
     * Kiểm tra Gemini Vision đã được cấu hình chưa
     */
    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_GEMINI_API_KEY_HERE");
    }
}
