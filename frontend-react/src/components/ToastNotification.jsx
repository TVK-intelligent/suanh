import { useState, useEffect, useCallback } from "react";

/**
 * Toast Notification System
 * Hiển thị thông báo dạng toast ở góc trên bên phải
 * Types: success, error, warning, info
 */

// Toast container - quản lý danh sách toast
function ToastContainer({ toasts, onRemove }) {
  return (
    <div className="toast-container">
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onRemove={onRemove} />
      ))}
    </div>
  );
}

// Toast item đơn lẻ
function ToastItem({ toast, onRemove }) {
  const [isExiting, setIsExiting] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsExiting(true);
      setTimeout(() => onRemove(toast.id), 300);
    }, toast.duration || 4000);

    return () => clearTimeout(timer);
  }, [toast, onRemove]);

  const handleClose = () => {
    setIsExiting(true);
    setTimeout(() => onRemove(toast.id), 300);
  };

  const icons = {
    success: "✅",
    error: "❌",
    warning: "⚠️",
    info: "ℹ️",
  };

  return (
    <div
      className={`toast-item toast-${toast.type} ${isExiting ? "toast-exit" : "toast-enter"}`}
    >
      <span className="toast-icon">{icons[toast.type] || icons.info}</span>
      <span className="toast-message">{toast.message}</span>
      <button className="toast-close" onClick={handleClose}>
        ✕
      </button>
    </div>
  );
}

// Custom hook để sử dụng toast
let toastIdCounter = 0;

export function useToast() {
  const [toasts, setToasts] = useState([]);

  const addToast = useCallback((message, type = "info", duration = 4000) => {
    const id = ++toastIdCounter;
    setToasts((prev) => [...prev, { id, message, type, duration }]);
  }, []);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const success = useCallback((msg) => addToast(msg, "success"), [addToast]);
  const error = useCallback((msg) => addToast(msg, "error", 6000), [addToast]);
  const warning = useCallback(
    (msg) => addToast(msg, "warning", 5000),
    [addToast],
  );
  const info = useCallback((msg) => addToast(msg, "info"), [addToast]);

  return { toasts, removeToast, success, error, warning, info };
}

export default ToastContainer;
