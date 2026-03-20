import { useState, useEffect } from "react";
import UploadForm from "./components/UploadForm";
import ImageComparison from "./components/ImageComparison";
import DetectionTags from "./components/DetectionTags";
import LoadingSpinner from "./components/LoadingSpinner";
import HistoryList from "./components/HistoryList";
import StatsBoard from "./components/StatsBoard";
import ImageZoomModal from "./components/ImageZoomModal";
import ToastContainer, { useToast } from "./components/ToastNotification";
import api from "./services/api";

/**
 * Main Application Component
 * AI Image Processor - Khử nhiễu & Nhận diện ảnh
 */
function App() {
  // State
  const [activeTab, setActiveTab] = useState("denoise"); // 'denoise' | 'detect' | 'full'
  const [isLoading, setIsLoading] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState("");
  const [uploadProgress, setUploadProgress] = useState(0);
  const [denoiseResult, setDenoiseResult] = useState(null);
  const [detectResult, setDetectResult] = useState(null);
  const [fullResult, setFullResult] = useState(null);
  const [error, setError] = useState(null);
  const [history, setHistory] = useState([]);
  const [backendStatus, setBackendStatus] = useState("checking");
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [speechSupported, setSpeechSupported] = useState(true);
  const [zoomImage, setZoomImage] = useState(null);
  const [detectionMode, setDetectionMode] = useState("gemini"); // 'gemini' | 'yolo'
  const toast = useToast();

  // Check backend health on mount
  useEffect(() => {
    checkBackendHealth();
    loadHistory();
  }, []);

  // Text-to-Speech handler
  const handleTextToSpeech = (text) => {
    if (!("speechSynthesis" in window)) {
      setSpeechSupported(false);
      toast.warning(
        "Trình duyệt không hỗ trợ đọc văn bản. Hãy dùng Chrome hoặc Edge.",
      );
      return;
    }

    const synth = window.speechSynthesis;

    if (isSpeaking) {
      synth.cancel();
      setIsSpeaking(false);
      return;
    }

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "vi-VN";
    utterance.rate = 0.9;
    utterance.pitch = 1.0;

    utterance.onstart = () => setIsSpeaking(true);
    utterance.onend = () => setIsSpeaking(false);
    utterance.onerror = () => {
      setIsSpeaking(false);
      toast.error("Có lỗi khi đọc văn bản. Vui lòng thử lại.");
    };

    synth.speak(utterance);
  };

  const checkBackendHealth = async () => {
    try {
      await api.healthCheck();
      setBackendStatus("online");
    } catch (err) {
      setBackendStatus("offline");
    }
  };

  const loadHistory = async (keyword = "") => {
    try {
      const data = await api.getHistory(0, 10, keyword);
      setHistory(data.images || []);
    } catch (err) {
      console.error("Failed to load history:", err);
    }
  };

  const handleUpload = async (file, strength = 0) => {
    setIsLoading(true);
    setError(null);
    setUploadProgress(0);

    const mode = activeTab;
    const messages = {
      denoise: "Đang tải ảnh lên để khử nhiễu...",
      detect: "Đang tải ảnh lên để nhận diện...",
      full: "Đang tải ảnh lên để xử lý toàn diện...",
    };
    setLoadingMessage(messages[mode]);

    try {
      let response;
      const onProgress = (progress) => {
        setUploadProgress(progress);
        if (progress < 100) {
          setLoadingMessage(`Đang tải ảnh lên... ${progress}%`);
        } else {
          const processingMessages = {
            denoise: `✨ Đang khử nhiễu ảnh (cường độ: ${strength})...`,
            detect: "🤖 Đang nhận diện đối tượng...",
            full: "🚀 Đang khử nhiễu + nhận diện...",
          };
          setLoadingMessage(processingMessages[mode]);
        }
      };

      if (mode === "denoise") {
        response = await api.denoiseImage(file, onProgress, strength);
      } else if (mode === "detect") {
        response = await api.detectImage(file, onProgress, detectionMode);
      } else {
        response = await api.uploadImage(file, onProgress);
      }

      if (response.status === "COMPLETED") {
        if (mode === "denoise") {
          setDenoiseResult(response);
          toast.success(
            `Khử nhiễu thành công trong ${response.processingTimeMs}ms!`,
          );
        } else if (mode === "detect") {
          setDetectResult(response);
          toast.success(
            `Nhận diện ${response.detectedObjects?.length || 0} đối tượng!`,
          );
          if (response.imageCaption && "speechSynthesis" in window) {
            setTimeout(() => handleTextToSpeech(response.imageCaption), 500);
          }
        } else {
          setFullResult(response);
          toast.success(
            `Xử lý toàn diện hoàn tất trong ${response.processingTimeMs}ms!`,
          );
          if (response.imageCaption && "speechSynthesis" in window) {
            setTimeout(() => handleTextToSpeech(response.imageCaption), 500);
          }
        }
        setLoadingMessage("");
        loadHistory();
      } else if (response.status === "FAILED") {
        setError(response.errorMessage || "Xử lý thất bại");
        toast.error(response.errorMessage || "Xử lý thất bại");
      }
    } catch (err) {
      console.error("Upload error:", err);
      const errorMsg =
        err.response?.data?.errorMessage ||
        err.message ||
        "Có lỗi xảy ra khi kết nối server";
      setError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setIsLoading(false);
      setLoadingMessage("");
    }
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setError(null);
  };

  const handleHistorySelect = (item) => {
    if (item.processedUrl && item.detectedObjects?.length > 0) {
      setActiveTab("full");
      setFullResult(item);
    } else if (item.processedUrl && !item.detectedObjects?.length) {
      setActiveTab("denoise");
      setDenoiseResult(item);
    } else if (item.detectedObjects?.length > 0) {
      setActiveTab("detect");
      setDetectResult(item);
    } else {
      setDenoiseResult(item);
    }
    setError(null);
    toast.info("Đã tải lại kết quả từ lịch sử");
  };

  const handleImageClick = (imageUrl) => {
    setZoomImage(imageUrl);
  };

  return (
    <div className="app">
      {/* Toast Notifications */}
      <ToastContainer toasts={toast.toasts} onRemove={toast.removeToast} />

      {/* Image Zoom Modal */}
      <ImageZoomModal
        imageUrl={zoomImage}
        isOpen={!!zoomImage}
        onClose={() => setZoomImage(null)}
      />

      {/* Loading Overlay */}
      {isLoading && (
        <LoadingSpinner
          message={loadingMessage}
          progress={uploadProgress < 100 ? uploadProgress : undefined}
        />
      )}

      {/* Header */}
      <header className="header">
        <div className="container header-content">
          <div className="logo">
            <div className="logo-icon">🖼️</div>
            <div className="logo-text">
              <h1>AI Image Processor</h1>
              <p>Khử nhiễu & Nhận diện ảnh thông minh</p>
            </div>
          </div>

          <div className="status-badge">
            <span className={`status-dot ${backendStatus}`}></span>
            <span>
              {backendStatus === "online"
                ? "Backend Online"
                : backendStatus === "offline"
                  ? "Backend Offline"
                  : "Đang kết nối..."}
            </span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="main">
        <div className="container">
          {/* Statistics Dashboard */}
          <StatsBoard />

          {/* Tab Navigation - 3 tabs */}
          <div className="tab-nav three-tabs">
            <button
              className={`tab-btn ${activeTab === "denoise" ? "active" : ""}`}
              onClick={() => handleTabChange("denoise")}
            >
              <span className="tab-icon">✨</span>
              <span className="tab-label">Khử nhiễu</span>
              <span className="tab-desc">Nâng cao chất lượng ảnh</span>
            </button>
            <button
              className={`tab-btn ${activeTab === "detect" ? "active" : ""}`}
              onClick={() => handleTabChange("detect")}
            >
              <span className="tab-icon">🔍</span>
              <span className="tab-label">Nhận diện</span>
              <span className="tab-desc">Phân tích bằng YOLO + AI</span>
            </button>
            <button
              className={`tab-btn ${activeTab === "full" ? "active" : ""}`}
              onClick={() => handleTabChange("full")}
            >
              <span className="tab-icon">🚀</span>
              <span className="tab-label">Toàn diện</span>
              <span className="tab-desc">Khử nhiễu + Nhận diện</span>
            </button>
          </div>

          {/* Error Message */}
          {error && (
            <div
              className="card fade-in mb-lg"
              style={{
                borderColor: "var(--error)",
                background: "rgba(239, 68, 68, 0.1)",
              }}
            >
              <p
                className="text-error"
                style={{
                  margin: 0,
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                }}
              >
                <span>⚠️</span> {error}
              </p>
            </div>
          )}

          {/* Upload Form */}
          <UploadForm
            onUpload={handleUpload}
            isLoading={isLoading}
            mode={activeTab}
          />

          {/* Detection Mode Selector - Only for detect tab */}
          {activeTab === "detect" && (
            <div className="card fade-in" style={{ marginTop: "1.5rem" }}>
              <h3 className="card-title">
                <span>⚙️</span>
                Chọn công cụ nhận diện
              </h3>
              <div
                style={{
                  display: "flex",
                  gap: "1rem",
                  flexWrap: "wrap",
                }}
              >
                <button
                  className={`detection-mode-btn ${
                    detectionMode === "gemini" ? "active" : ""
                  }`}
                  onClick={() => setDetectionMode("gemini")}
                  style={{
                    flex: "1",
                    minWidth: "150px",
                    padding: "1rem",
                    borderRadius: "8px",
                    border: "2px solid",
                    borderColor:
                      detectionMode === "gemini"
                        ? "var(--primary)"
                        : "rgba(59, 130, 246, 0.2)",
                    background:
                      detectionMode === "gemini"
                        ? "rgba(59, 130, 246, 0.1)"
                        : "transparent",
                    color: "inherit",
                    cursor: "pointer",
                    fontWeight: detectionMode === "gemini" ? "600" : "500",
                    transition: "all 200ms ease",
                  }}
                >
                  <div style={{ fontSize: "1.5rem", marginBottom: "0.5rem" }}>
                    🤖
                  </div>
                  <div style={{ fontWeight: "600" }}>YOLO + AI</div>
                  <div style={{ fontSize: "0.85rem", opacity: 0.7 }}>
                    API-based, chi tiết
                  </div>
                </button>

                <button
                  className={`detection-mode-btn ${
                    detectionMode === "yolo" ? "active" : ""
                  }`}
                  onClick={() => setDetectionMode("yolo")}
                  style={{
                    flex: "1",
                    minWidth: "150px",
                    padding: "1rem",
                    borderRadius: "8px",
                    border: "2px solid",
                    borderColor:
                      detectionMode === "yolo"
                        ? "var(--primary)"
                        : "rgba(59, 130, 246, 0.2)",
                    background:
                      detectionMode === "yolo"
                        ? "rgba(59, 130, 246, 0.1)"
                        : "transparent",
                    color: "inherit",
                    cursor: "pointer",
                    fontWeight: detectionMode === "yolo" ? "600" : "500",
                    transition: "all 200ms ease",
                  }}
                >
                  <div style={{ fontSize: "1.5rem", marginBottom: "0.5rem" }}>
                    ⚡
                  </div>
                  <div style={{ fontWeight: "600" }}>YOLO v5</div>
                  <div style={{ fontSize: "0.85rem", opacity: 0.7 }}>
                    Local model, nhanh
                  </div>
                </button>
              </div>

            </div>
          )}

          {/* === DENOISE TAB RESULTS === */}
          {activeTab === "denoise" && denoiseResult && (
            <div
              onClick={() =>
                handleImageClick(
                  denoiseResult.processedUrl || denoiseResult.originalUrl,
                )
              }
            >
              <ImageComparison
                originalUrl={denoiseResult.originalUrl}
                processedUrl={denoiseResult.processedUrl}
                isLoading={isLoading}
                psnr={denoiseResult.psnr}
                ssim={denoiseResult.ssim}
              />
            </div>
          )}

          {/* === DETECT TAB RESULTS === */}
          {activeTab === "detect" && detectResult && (
            <>
              {detectResult.originalUrl && (
                <div className="card fade-in mt-xl">
                  <h3 className="card-title">
                    <span>📷</span>
                    Ảnh đã tải lên
                  </h3>
                  <div
                    className="detect-image-container clickable"
                    onClick={() => handleImageClick(detectResult.originalUrl)}
                  >
                    <img src={detectResult.originalUrl} alt="Ảnh phân tích" />
                    <div className="image-zoom-hint">🔍 Click để phóng to</div>
                  </div>
                </div>
              )}

              {detectResult.imageCaption && (
                <div className="card fade-in mt-xl caption-card">
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      marginBottom: "1rem",
                    }}
                  >
                    <h3 className="card-title" style={{ margin: 0 }}>
                      <span>📝</span>
                      Mô tả ảnh Yolo5
                    </h3>
                    <button
                      className={`tts-button ${isSpeaking ? "speaking" : ""}`}
                      onClick={() =>
                        handleTextToSpeech(detectResult.imageCaption)
                      }
                      title={isSpeaking ? "Dừng đọc" : "Đọc mô tả"}
                      disabled={!speechSupported}
                    >
                      <span className="tts-icon">
                        {isSpeaking ? "🔇" : "🔊"}
                      </span>
                      <span className="tts-text">
                        {isSpeaking ? "Đang đọc..." : "Đọc mô tả"}
                      </span>
                    </button>
                  </div>
                  <p className="caption-text">{detectResult.imageCaption}</p>
                </div>
              )}

              {(!detectResult.detectedObjects || detectResult.detectedObjects.length === 0) && (
                <div className="card fade-in mt-xl" style={{ 
                  borderColor: "var(--warning)",
                  background: "rgba(245, 158, 11, 0.1)" 
                }}>
                  <p style={{ 
                    margin: 0, 
                    display: "flex", 
                    alignItems: "center",
                    gap: "0.5rem",
                    color: "var(--warning-text)",
                    fontWeight: "500"
                  }}>
                    <span>⚠️</span> 
                    {detectionMode === "yolo" 
                      ? "YOLO không phát hiện đối tượng nào trong ảnh. Hãy thử thay đổi ảnh hoặc dùng Gemini Vision để có kết quả chi tiết hơn."
                      : "Không phát hiện đối tượng nào trong ảnh. Vui lòng thử ảnh khác."}
                  </p>
                </div>
              )}

              <DetectionTags
                tags={detectResult.detectedObjects}
                yoloLabels={detectResult.yoloLabels}
                processingTime={detectResult.processingTimeMs}
                imageWidth={detectResult.imageWidth}
                imageHeight={detectResult.imageHeight}
              />
            </>
          )}

          {/* === FULL ANALYSIS TAB RESULTS === */}
          {activeTab === "full" && fullResult && (
            <>
              {/* Image Comparison */}
              {fullResult.processedUrl && (
                <div onClick={() => handleImageClick(fullResult.processedUrl)}>
                  <ImageComparison
                    originalUrl={fullResult.originalUrl}
                    processedUrl={fullResult.processedUrl}
                    isLoading={isLoading}
                    psnr={fullResult.psnr}
                    ssim={fullResult.ssim}
                  />
                </div>
              )}

              {/* Caption */}
              {fullResult.imageCaption && (
                <div className="card fade-in mt-xl caption-card">
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      marginBottom: "1rem",
                    }}
                  >
                    <h3 className="card-title" style={{ margin: 0 }}>
                      <span>📝</span>
                      Mô tả ảnh (YOLO + AI)
                    </h3>
                    <button
                      className={`tts-button ${isSpeaking ? "speaking" : ""}`}
                      onClick={() =>
                        handleTextToSpeech(fullResult.imageCaption)
                      }
                      title={isSpeaking ? "Dừng đọc" : "Đọc mô tả"}
                      disabled={!speechSupported}
                    >
                      <span className="tts-icon">
                        {isSpeaking ? "🔇" : "🔊"}
                      </span>
                      <span className="tts-text">
                        {isSpeaking ? "Đang đọc..." : "Đọc mô tả"}
                      </span>
                    </button>
                  </div>
                  <p className="caption-text">{fullResult.imageCaption}</p>
                </div>
              )}

              {/* Detection Tags */}
              <DetectionTags
                tags={fullResult.detectedObjects}
                yoloLabels={fullResult.yoloLabels}
                processingTime={fullResult.processingTimeMs}
                imageWidth={fullResult.imageWidth}
                imageHeight={fullResult.imageHeight}
              />
            </>
          )}

          {/* History */}
          <HistoryList
            history={history}
            onSelect={handleHistorySelect}
            onHistoryChange={loadHistory}
          />
        </div>
      </main>

      {/* Footer */}
      <footer className="footer">
        <p>
          Powered by <strong>Spring Boot</strong> +{" "}
          <strong>Yolo5 API</strong> + <strong>React</strong>
        </p>
        <p style={{ marginTop: "0.5rem" }}>
          ✨ OpenCV Denoise • 🔍 Yolo5 AI (Nhận diện) • 🚀 Full Analysis
        </p>
      </footer>
    </div>
  );
}

export default App;
