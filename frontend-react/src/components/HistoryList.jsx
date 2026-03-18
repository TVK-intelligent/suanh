import { useState } from "react";
import api from "../services/api";

/**
 * Component hiển thị lịch sử xử lý ảnh
 * Hỗ trợ tìm kiếm, xóa từng item và xóa tất cả
 */
function HistoryList({ history, onSelect, onHistoryChange }) {
  const [searchKeyword, setSearchKeyword] = useState("");

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const handleDelete = async (e, id) => {
    e.stopPropagation();
    if (!confirm("Bạn có chắc muốn xóa ảnh này?")) return;

    try {
      await api.deleteImage(id);
      onHistoryChange && onHistoryChange();
    } catch (err) {
      console.error("Delete failed:", err);
      alert("Xóa thất bại. Vui lòng thử lại.");
    }
  };

  const handleClearAll = async () => {
    if (!history || history.length === 0) return;
    if (!confirm(`Bạn có chắc muốn xóa tất cả ${history.length} ảnh?`)) return;

    try {
      await api.clearHistory();
      onHistoryChange && onHistoryChange();
    } catch (err) {
      console.error("Clear history failed:", err);
      alert("Xóa thất bại. Vui lòng thử lại.");
    }
  };

  const handleSearch = (e) => {
    const keyword = e.target.value;
    setSearchKeyword(keyword);
    // Trigger tìm kiếm qua callback
    onHistoryChange && onHistoryChange(keyword);
  };

  const getStatusInfo = (status) => {
    switch (status) {
      case "COMPLETED":
        return { text: "✓ Hoàn thành", cls: "completed" };
      case "FAILED":
        return { text: "✗ Lỗi", cls: "failed" };
      case "PROCESSING":
        return { text: "⏳ Đang xử lý", cls: "processing" };
      default:
        return { text: "Chờ", cls: "pending" };
    }
  };

  const getTypeTag = (item) => {
    if (item.processedUrl && item.detectedObjects?.length > 0)
      return { text: "🚀 Full", cls: "type-full" };
    if (item.processedUrl) return { text: "✨ Khử nhiễu", cls: "type-denoise" };
    if (item.detectedObjects?.length > 0)
      return { text: "🔍 Nhận diện", cls: "type-detect" };
    return { text: "📷 Ảnh", cls: "type-default" };
  };

  return (
    <div className="card mt-xl fade-in">
      <div className="history-header">
        <h3 className="card-title" style={{ margin: 0 }}>
          <span>📜</span>
          Lịch sử xử lý{" "}
          {history && history.length > 0 ? `(${history.length})` : ""}
        </h3>
        {history && history.length > 0 && (
          <button
            className="clear-all-btn"
            onClick={handleClearAll}
            title="Xóa tất cả"
          >
            <span>🗑️</span>
            <span>Xóa tất cả</span>
          </button>
        )}
      </div>

      {/* Search Bar */}
      <div className="history-search">
        <span className="history-search-icon">🔍</span>
        <input
          type="text"
          placeholder="Tìm kiếm theo tên file..."
          value={searchKeyword}
          onChange={handleSearch}
          className="history-search-input"
        />
        {searchKeyword && (
          <button
            className="history-search-clear"
            onClick={() => {
              setSearchKeyword("");
              onHistoryChange && onHistoryChange("");
            }}
          >
            ✕
          </button>
        )}
      </div>

      {!history || history.length === 0 ? (
        <p
          className="text-center"
          style={{ color: "var(--text-muted)", padding: "2rem" }}
        >
          {searchKeyword
            ? `Không tìm thấy kết quả cho "${searchKeyword}"`
            : "Chưa có ảnh nào được xử lý"}
        </p>
      ) : (
        <div className="history-list">
          {history.map((item) => {
            const statusInfo = getStatusInfo(item.status);
            const typeTag = getTypeTag(item);

            return (
              <div
                key={item.id}
                className="history-item"
                onClick={() => onSelect && onSelect(item)}
              >
                <img
                  src={
                    item.processedUrl || item.originalUrl || "/placeholder.png"
                  }
                  alt={item.originalFilename}
                  className="history-thumbnail"
                  onError={(e) => {
                    e.target.style.display = "none";
                  }}
                />

                <div className="history-info">
                  <div className="history-filename">
                    {item.originalFilename || "Unknown"}
                  </div>
                  <div className="history-meta">
                    {formatDate(item.createdAt)}
                    {item.processingTimeMs && ` • ${item.processingTimeMs}ms`}
                  </div>
                  <span className={`history-type-tag ${typeTag.cls}`}>
                    {typeTag.text}
                  </span>
                </div>

                <span className={`history-status ${statusInfo.cls}`}>
                  {statusInfo.text}
                </span>

                <button
                  className="history-delete-btn"
                  onClick={(e) => handleDelete(e, item.id)}
                  title="Xóa"
                >
                  ✕
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default HistoryList;
