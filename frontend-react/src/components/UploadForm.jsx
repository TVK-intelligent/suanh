import { useState, useRef } from "react";

/**
 * Component Upload ảnh với Drag & Drop
 * Hiển thị text khác nhau tùy theo mode (denoise/detect/full)
 * Hỗ trợ tuỳ chỉnh cường độ khử nhiễu
 */
function UploadForm({ onUpload, isLoading, mode, compact = false }) {
  const [isDragOver, setIsDragOver] = useState(false);
  const [denoiseStrength, setDenoiseStrength] = useState(10);
  const fileInputRef = useRef(null);

  const isDenoise = mode === "denoise";
  const isDetect = mode === "detect" || !mode;
  const isFull = mode === "full";

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);

    const files = e.dataTransfer.files;
    if (files.length > 0) {
      handleFile(files[0]);
    }
  };

  const handleFileSelect = (e) => {
    const files = e.target.files;
    if (files.length > 0) {
      handleFile(files[0]);
    }
  };

  const handleFile = (file) => {
    if (!file.type.startsWith("image/")) {
      alert("Vui lòng chọn file ảnh (JPEG, PNG, WebP, BMP)");
      return;
    }

    if (file.size > 50 * 1024 * 1024) {
      alert("File quá lớn. Tối đa 50MB");
      return;
    }

    onUpload(file, isDenoise || isFull ? denoiseStrength : 0);
  };

  const handleClick = () => {
    if (!isLoading) {
      fileInputRef.current?.click();
    }
  };

  const getTitle = () => {
    if (isFull) return "Tải ảnh lên để xử lý toàn diện";
    if (isDetect) return "Tải ảnh lên để nhận diện";
    return "Tải ảnh lên để khử nhiễu";
  };

  const getIcon = () => {
    if (isFull) return "🚀";
    if (isDetect) return "🔍";
    return "✨";
  };

  const getUploadIcon = () => {
    if (isDragOver) return "📥";
    if (isFull) return "🚀";
    if (isDetect) return "🤖";
    return "🖼️";
  };

  const getUploadText = () => {
    if (isDragOver) return "Thả ảnh vào đây";
    if (isFull) return "Kéo thả ảnh để xử lý khử nhiễu + nhận diện";
    if (isDetect) return "Kéo thả ảnh cần nhận diện hoặc click để chọn";
    return "Kéo thả ảnh cần khử nhiễu hoặc click để chọn";
  };

  const getHintText = () => {
    if (isFull)
      return "AI sẽ khử nhiễu bằng OpenCV và nhận diện bằng Gemini Vision";
    if (isDetect)
      return "Gemini AI sẽ phân tích và nhận diện đối tượng trong ảnh";
    return "AI sẽ nâng cao chất lượng ảnh bằng OpenCV Denoising";
  };

  const getStrengthLabel = (value) => {
    if (value <= 5) return "Nhẹ";
    if (value <= 10) return "Vừa";
    if (value <= 20) return "Mạnh";
    return "Rất mạnh";
  };

  return (
    <div className="card fade-in" style={compact ? { all: "unset" } : {}}>
      {!compact && (
        <h3 className="card-title">
          <span>{getIcon()}</span>
          {getTitle()}
        </h3>
      )}

      <div
        className={`upload-zone ${isDragOver ? "drag-over" : ""}`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={handleClick}
        role="button"
        tabIndex={0}
        style={
          compact
            ? {
                border: "2px dashed rgba(59, 130, 246, 0.3)",
                borderRadius: "12px",
                padding: "60px 40px",
                cursor: "pointer",
                transition: "all 200ms ease",
              }
            : {}
        }
      >
        <span className="upload-icon">{getUploadIcon()}</span>
        <p className="upload-text">{getUploadText()}</p>
        {!compact && <p className="upload-hint">{getHintText()}</p>}
        {!compact && (
          <p className="upload-hint" style={{ marginTop: "0.25rem" }}>
            Hỗ trợ: JPEG, PNG, WebP, BMP • Tối đa 50MB
          </p>
        )}

        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp,image/bmp"
          onChange={handleFileSelect}
          style={{ display: "none" }}
        />
      </div>

      {/* Denoise Strength Slider - chỉ hiển thị cho mode denoise và full */}
      {!compact && (isDenoise || isFull) && (
        <div className="strength-slider" onClick={(e) => e.stopPropagation()}>
          <div className="strength-header">
            <span className="strength-label">
              🎚️ Cường độ khử nhiễu: <strong>{denoiseStrength}</strong>
            </span>
            <span
              className={`strength-badge strength-${getStrengthLabel(denoiseStrength).toLowerCase()}`}
            >
              {getStrengthLabel(denoiseStrength)}
            </span>
          </div>
          <input
            type="range"
            min="1"
            max="30"
            value={denoiseStrength}
            onChange={(e) => setDenoiseStrength(parseInt(e.target.value))}
            className="strength-range"
          />
          <div className="strength-marks">
            <span>1 (Nhẹ)</span>
            <span>10 (Vừa)</span>
            <span>20 (Mạnh)</span>
            <span>30 (Max)</span>
          </div>
          <p className="strength-desc">
            💡 Giá trị cao = ảnh mượt hơn nhưng có thể mất chi tiết
          </p>
        </div>
      )}
    </div>
  );
}

export default UploadForm;
