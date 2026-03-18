import { useState, useEffect } from "react";
import api from "../services/api";

/**
 * Component Dashboard thống kê tổng quan
 * Hiển thị: tổng ảnh, tỷ lệ thành công, thời gian TB, dung lượng
 */
function StatsBoard() {
  const [stats, setStats] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const data = await api.getStats();
      setStats(data);
    } catch (err) {
      console.error("Failed to load stats:", err);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading || !stats) {
    return null;
  }

  // Không hiển thị nếu chưa có ảnh nào
  if (stats.totalImages === 0) {
    return null;
  }

  return (
    <div className="stats-board fade-in">
      <div className="stats-board-header">
        <h3 className="stats-board-title">
          <span>📊</span>
          Thống kê tổng quan
        </h3>
        <button
          className="stats-refresh-btn"
          onClick={loadStats}
          title="Làm mới thống kê"
        >
          🔄
        </button>
      </div>

      <div className="stats-board-grid">
        <div className="stats-board-item">
          <div className="stats-board-icon">🖼️</div>
          <div className="stats-board-value">{stats.totalImages}</div>
          <div className="stats-board-label">Tổng ảnh</div>
        </div>

        <div className="stats-board-item">
          <div className="stats-board-icon">📅</div>
          <div className="stats-board-value">{stats.todayCount}</div>
          <div className="stats-board-label">Hôm nay</div>
        </div>

        <div className="stats-board-item success">
          <div className="stats-board-icon">✅</div>
          <div className="stats-board-value">{stats.successRate}%</div>
          <div className="stats-board-label">Tỷ lệ thành công</div>
        </div>

        <div className="stats-board-item">
          <div className="stats-board-icon">⚡</div>
          <div className="stats-board-value">{stats.avgProcessingTimeMs}ms</div>
          <div className="stats-board-label">Thời gian TB</div>
        </div>

        <div className="stats-board-item">
          <div className="stats-board-icon">💾</div>
          <div className="stats-board-value">{stats.totalFileSizeMB} MB</div>
          <div className="stats-board-label">Dung lượng</div>
        </div>

        {stats.failedCount > 0 && (
          <div className="stats-board-item error">
            <div className="stats-board-icon">❌</div>
            <div className="stats-board-value">{stats.failedCount}</div>
            <div className="stats-board-label">Thất bại</div>
          </div>
        )}
      </div>
    </div>
  );
}

export default StatsBoard;
