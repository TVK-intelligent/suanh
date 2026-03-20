package com.app.service;

import jakarta.annotation.PostConstruct;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * YOLOv5 object detection service (OpenCV DNN + ONNX).
 */
@Service
public class YoloDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(YoloDetectionService.class);

    private static final String[] COCO_LABELS = {
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
            "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
            "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
            "tennis racket", "bottle",
            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
            "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch", "potted plant", "bed",
            "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", "oven",
            "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
            "toothbrush"
    };

    @Value("${app.yolo.enabled:true}")
    private boolean enabled;

    @Value("${app.yolo.model-path:./src/main/resources/models/yolov5s.onnx}")
    private String modelPath;

    @Value("${app.yolo.confidence-threshold:0.35}")
    private float confidenceThreshold;

    @Value("${app.yolo.nms-threshold:0.45}")
    private float nmsThreshold;

    private Net net;
    private boolean modelLoaded = false;

    @PostConstruct
    public void init() {
        if (!enabled) {
            logger.info("YOLO detection disabled by config.");
            return;
        }

        try {
            nu.pattern.OpenCV.loadLocally();

            Path resolvedModelPath = resolveModelPath();
            if (resolvedModelPath == null || !Files.exists(resolvedModelPath)) {
                logger.warn("YOLO model not found. Expected at '{}', or classpath '/models/yolov5s.onnx'.", modelPath);
                return;
            }

            net = Dnn.readNetFromONNX(resolvedModelPath.toString());
            modelLoaded = !net.empty();

            if (modelLoaded) {
                logger.info("✓ YOLO model loaded: {}", resolvedModelPath);
            } else {
                logger.warn("YOLO model loaded but net is empty.");
            }
        } catch (Exception e) {
            logger.error("Failed to init YOLO service: {}", e.getMessage(), e);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    /**
     * Detect objects from image bytes.
     */
    public List<String> detectObjects(byte[] imageData) {
        if (!enabled || !modelLoaded || imageData == null || imageData.length == 0) {
            logger.warn("YOLO detection skipped: enabled={}, modelLoaded={}, imageDataLength={}", 
                enabled, modelLoaded, imageData != null ? imageData.length : 0);
            return Collections.emptyList();
        }

        Mat image = new Mat();
        Mat blob = new Mat();
        Mat output = new Mat();
        Mat detections = new Mat();

        try {
            image = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
            if (image.empty()) {
                logger.warn("Failed to decode image from bytes");
                return Collections.emptyList();
            }

            int originalWidth = image.cols();
            int originalHeight = image.rows();
            logger.info("Image decoded: {}x{}", originalWidth, originalHeight);

            blob = Dnn.blobFromImage(
                    image,
                    1.0 / 255.0,
                    new Size(640, 640),
                    new Scalar(0, 0, 0),
                    true,
                    false);

            net.setInput(blob);
            output = net.forward();
            logger.info("YOLO forward pass completed. Output shape: {}x{}", output.rows(), output.cols());

            int attributes = 85; // x,y,w,h,obj + 80 classes
            int rows;
            if (output.cols() == attributes) {
                detections = output;
                rows = output.rows();
            } else {
                rows = (int) (output.total() / attributes);
                detections = output.reshape(1, rows);
            }

            logger.info("Processing {} detection rows with threshold={}", rows, confidenceThreshold);

            List<Rect2d> boxes = new ArrayList<>();
            List<Float> confidences = new ArrayList<>();
            List<Integer> classIds = new ArrayList<>();

            double scaleX = originalWidth / 640.0;
            double scaleY = originalHeight / 640.0;
            
            // Track max scores for debugging
            float maxObjectness = 0;
            float maxConfidence = 0;

            for (int i = 0; i < rows; i++) {
                float[] rowData = new float[attributes];
                detections.get(i, 0, rowData);

                float objectness = rowData[4];
                maxObjectness = Math.max(maxObjectness, objectness);
                
                // Find best class
                int bestClassId = -1;
                float bestClassScore = 0f;
                for (int c = 5; c < attributes; c++) {
                    float score = rowData[c];
                    if (score > bestClassScore) {
                        bestClassScore = score;
                        bestClassId = c - 5;
                    }
                }

                // Combined confidence = objectness * class score
                float confidence = objectness * bestClassScore;
                maxConfidence = Math.max(maxConfidence, confidence);
                
                // Log top detections for debugging
                if (i < 5) {
                    logger.info("Row {}: obj={}, best_class_id={}, class_score={}, confidence={}", i, String.format("%.4f", objectness), bestClassId, String.format("%.4f", bestClassScore), String.format("%.4f", confidence));
                }
                
                // Single confidence threshold check
                if (confidence < confidenceThreshold || bestClassId < 0) {
                    continue;
                }

                double centerX = rowData[0] * scaleX;
                double centerY = rowData[1] * scaleY;
                double width = rowData[2] * scaleX;
                double height = rowData[3] * scaleY;

                double left = Math.max(0, centerX - width / 2.0);
                double top = Math.max(0, centerY - height / 2.0);

                boxes.add(new Rect2d(left, top, Math.max(1, width), Math.max(1, height)));
                confidences.add(confidence);
                classIds.add(bestClassId);
                logger.info("✓ Detection added - class {} ({}): confidence {}", bestClassId, COCO_LABELS[bestClassId], String.format("%.4f", confidence));
            }
            
            logger.info("Max scores - objectness: {}, confidence: {}", String.format("%.4f", maxObjectness), String.format("%.4f", maxConfidence));

            logger.info("Found {} detections before NMS", boxes.size());

            if (boxes.isEmpty()) {
                logger.warn("No objects detected by YOLO");
                return Collections.emptyList();
            }

            MatOfRect2d boxesMat = new MatOfRect2d();
            boxesMat.fromList(boxes);
            MatOfFloat confMat = new MatOfFloat();
            confMat.fromArray(toPrimitive(confidences));
            MatOfInt indices = new MatOfInt();

            Dnn.NMSBoxes(boxesMat, confMat, confidenceThreshold, nmsThreshold, indices);

            Set<String> labels = new LinkedHashSet<>();
            int[] keep = indices.toArray();
            logger.info("After NMS: {} detections kept", keep.length);
            
            for (int idx : keep) {
                if (idx >= 0 && idx < classIds.size()) {
                    int classId = classIds.get(idx);
                    if (classId >= 0 && classId < COCO_LABELS.length) {
                        labels.add(COCO_LABELS[classId]);
                        logger.debug("Detected: {} (confidence: {})", COCO_LABELS[classId], confidences.get(idx));
                    }
                }
            }

            logger.info("✓ YOLO detection result: {}", labels);
            return new ArrayList<>(labels);
        } catch (Exception e) {
            logger.error("YOLO detection failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            image.release();
            blob.release();
            output.release();
            if (detections != output) {
                detections.release();
            }
        }
    }

    private float[] toPrimitive(List<Float> values) {
        float[] arr = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            arr[i] = values.get(i);
        }
        return arr;
    }

    private Path resolveModelPath() throws IOException {
        Path configured = Paths.get(modelPath);
        if (Files.exists(configured)) {
            return configured;
        }

        ClassPathResource resource = new ClassPathResource("models/yolov5s.onnx");
        if (!resource.exists()) {
            return null;
        }

        Path tempFile = Files.createTempFile("yolov5s-", ".onnx");
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }
}
