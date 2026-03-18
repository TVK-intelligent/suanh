import { useEffect, useCallback } from "react";

/**
 * Component Modal xem ảnh phóng to toàn màn hình
 * - Click vào ảnh để mở
 * - Click overlay hoặc nút X để đóng
 * - Hỗ trợ phím ESC để đóng
 */
function ImageZoomModal({ imageUrl, altText, isOpen, onClose }) {
  const handleKeyDown = useCallback(
    (e) => {
      if (e.key === "Escape") {
        onClose();
      }
    },
    [onClose],
  );

  useEffect(() => {
    if (isOpen) {
      document.addEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "hidden";
    }
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "";
    };
  }, [isOpen, handleKeyDown]);

  if (!isOpen || !imageUrl) {
    return null;
  }

  return (
    <div className="zoom-overlay" onClick={onClose}>
      <div className="zoom-container" onClick={(e) => e.stopPropagation()}>
        <button className="zoom-close-btn" onClick={onClose} title="Đóng (ESC)">
          ✕
        </button>
        <img
          src={imageUrl}
          alt={altText || "Ảnh phóng to"}
          className="zoom-image"
          draggable="false"
        />
        <div className="zoom-info">
          <span>🔍 Click bên ngoài hoặc nhấn ESC để đóng</span>
        </div>
      </div>
    </div>
  );
}

export default ImageZoomModal;
