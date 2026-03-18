import { useState, useRef, useCallback } from 'react';

/**
 * Component so sánh ảnh trước/sau xử lý với Slider kéo qua lại
 * - Kéo thanh trượt để so sánh ảnh gốc và ảnh đã khử nhiễu
 * - Nút download ảnh đã xử lý
 * - Hiển thị chỉ số PSNR/SSIM
 */
function ImageComparison({ originalUrl, processedUrl, isLoading, psnr, ssim }) {
  const [sliderPosition, setSliderPosition] = useState(50);
  const [isDragging, setIsDragging] = useState(false);
  const containerRef = useRef(null);

  if (!originalUrl && !processedUrl && !isLoading) {
    return null;
  }

  const handleMove = useCallback((clientX) => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const x = clientX - rect.left;
    const percentage = Math.max(0, Math.min(100, (x / rect.width) * 100));
    setSliderPosition(percentage);
  }, []);

  const handleMouseDown = (e) => {
    e.preventDefault();
    setIsDragging(true);
    handleMove(e.clientX);

    const onMouseMove = (e) => handleMove(e.clientX);
    const onMouseUp = () => {
      setIsDragging(false);
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    };

    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  };

  const handleTouchStart = (e) => {
    setIsDragging(true);
    handleMove(e.touches[0].clientX);
  };

  const handleTouchMove = (e) => {
    e.preventDefault();
    handleMove(e.touches[0].clientX);
  };

  const handleTouchEnd = () => {
    setIsDragging(false);
  };

  const handleDownload = () => {
    if (!processedUrl) return;
    const link = document.createElement('a');
    link.href = processedUrl;
    link.download = 'processed_image.png';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Nếu chỉ có 1 ảnh (chưa xử lý xong), hiển thị đơn giản
  if (!processedUrl) {
    return (
      <div className="card fade-in mt-xl">
        <h3 className="card-title">
          <span>🔍</span>
          Kết quả xử lý
        </h3>
        <div className="image-comparison-simple">
          <div className="image-panel">
            <div className="image-panel-header">
              <span>📷</span>
              <span className="image-panel-title">Ảnh gốc</span>
            </div>
            <div className="image-panel-content">
              {originalUrl ? (
                <img src={originalUrl} alt="Ảnh gốc" loading="lazy" />
              ) : (
                <div className="image-placeholder">
                  {isLoading ? <span className="pulse">Đang tải...</span> : 'Chưa có ảnh'}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="card fade-in mt-xl">
      <div className="comparison-header">
        <h3 className="card-title" style={{ margin: 0 }}>
          <span>🔍</span>
          So sánh kết quả
        </h3>
        <button className="download-btn" onClick={handleDownload} title="Tải ảnh đã xử lý">
          <span>📥</span>
          <span>Tải ảnh</span>
        </button>
      </div>

      <p className="comparison-hint">👆 Kéo thanh trượt để so sánh ảnh gốc và ảnh đã xử lý</p>

      {/* Slider Comparison */}
      <div
        ref={containerRef}
        className={`comparison-slider-container ${isDragging ? 'dragging' : ''}`}
        onMouseDown={handleMouseDown}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
      >
        {/* Processed image (bottom layer - full width) */}
        <img
          src={processedUrl}
          alt="Ảnh đã xử lý"
          className="comparison-img comparison-img-processed"
          draggable="false"
        />

        {/* Original image (top layer - clipped) */}
        <div
          className="comparison-clip"
          style={{ clipPath: `inset(0 ${100 - sliderPosition}% 0 0)` }}
        >
          <img
            src={originalUrl}
            alt="Ảnh gốc"
            className="comparison-img comparison-img-original"
            draggable="false"
          />
        </div>

        {/* Labels */}
        <div className="comparison-label comparison-label-left">📷 Ảnh gốc</div>
        <div className="comparison-label comparison-label-right">✨ Đã xử lý</div>

        {/* Slider handle */}
        <div
          className="comparison-slider-line"
          style={{ left: `${sliderPosition}%` }}
        >
          <div className="comparison-slider-handle">
            <span>◀▶</span>
          </div>
        </div>
      </div>

      {/* Quality Metrics */}
      {(psnr || ssim) && (
        <div className="quality-metrics">
          <h4 className="metrics-title">
            <span>📊</span> Chỉ số chất lượng
          </h4>
          <div className="metrics-grid">
            {psnr && (
              <div className="metric-card">
                <div className="metric-value">{psnr} dB</div>
                <div className="metric-label">PSNR</div>
                <div className="metric-desc">
                  {psnr >= 40 ? '🟢 Xuất sắc' : psnr >= 30 ? '🟡 Tốt' : '🟠 Trung bình'}
                </div>
              </div>
            )}
            {ssim && (
              <div className="metric-card">
                <div className="metric-value">{ssim}</div>
                <div className="metric-label">SSIM</div>
                <div className="metric-desc">
                  {ssim >= 0.95 ? '🟢 Xuất sắc' : ssim >= 0.85 ? '🟡 Tốt' : '🟠 Trung bình'}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default ImageComparison;
