import axios from "axios";

// Base URL for API calls
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api/images";
const ASSET_BASE_URL = import.meta.env.VITE_ASSET_BASE_URL || "";

const getOriginFromApiBase = () => {
  try {
    if (
      API_BASE_URL.startsWith("http://") ||
      API_BASE_URL.startsWith("https://")
    ) {
      return new URL(API_BASE_URL).origin;
    }
  } catch {
    // no-op
  }
  return "";
};

const normalizeAssetUrl = (url) => {
  if (!url) return url;
  if (
    url.startsWith("http://") ||
    url.startsWith("https://") ||
    url.startsWith("data:")
  ) {
    return url;
  }

  const base = (ASSET_BASE_URL || getOriginFromApiBase()).replace(/\/$/, "");
  if (!base) return url;

  return `${base}/${url.replace(/^\//, "")}`;
};

const normalizeImagePayload = (payload) => {
  if (!payload || typeof payload !== "object") return payload;

  return {
    ...payload,
    originalUrl: normalizeAssetUrl(payload.originalUrl),
    processedUrl: normalizeAssetUrl(payload.processedUrl),
  };
};

/**
 * API Service cho Image Processing
 */
const api = {
  /**
   * Upload và xử lý ảnh (cả khử nhiễu + nhận diện)
   */
  uploadImage: async (file, onProgress) => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await axios.post(`${API_BASE_URL}/upload`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          onProgress(
            Math.round((progressEvent.loaded * 100) / progressEvent.total),
          );
        }
      },
    });

    return normalizeImagePayload(response.data);
  },

  /**
   * Chỉ khử nhiễu ảnh (hỗ trợ tuỳ chỉnh cường độ)
   */
  denoiseImage: async (file, onProgress, strength = 0) => {
    const formData = new FormData();
    formData.append("file", file);

    const params = strength > 0 ? `?strength=${strength}` : "";

    const response = await axios.post(
      `${API_BASE_URL}/denoise${params}`,
      formData,
      {
        headers: { "Content-Type": "multipart/form-data" },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            onProgress(
              Math.round((progressEvent.loaded * 100) / progressEvent.total),
            );
          }
        },
      },
    );

    return normalizeImagePayload(response.data);
  },

  /**
   * Chỉ nhận diện đối tượng (Gemini Vision hoặc YOLO)
   */
  detectImage: async (file, onProgress, mode = "gemini") => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("mode", mode);

    const response = await axios.post(`${API_BASE_URL}/detect`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          onProgress(
            Math.round((progressEvent.loaded * 100) / progressEvent.total),
          );
        }
      },
    });

    return normalizeImagePayload(response.data);
  },

  /**
   * Lấy thông tin ảnh theo ID
   */
  getImage: async (id) => {
    const response = await axios.get(`${API_BASE_URL}/${id}`);
    return normalizeImagePayload(response.data);
  },

  /**
   * Lấy lịch sử xử lý ảnh (hỗ trợ tìm kiếm)
   */
  getHistory: async (page = 0, size = 10, keyword = "") => {
    const params = { page, size };
    if (keyword && keyword.trim()) {
      params.keyword = keyword.trim();
    }
    const response = await axios.get(`${API_BASE_URL}/history`, { params });
    return {
      ...response.data,
      images: (response.data?.images || []).map(normalizeImagePayload),
    };
  },

  /**
   * Thống kê tổng quan
   */
  getStats: async () => {
    const response = await axios.get(`${API_BASE_URL}/stats`);
    return response.data;
  },

  /**
   * Health check
   */
  healthCheck: async () => {
    const response = await axios.get(`${API_BASE_URL}/health`);
    return response.data;
  },

  /**
   * Xóa một ảnh theo ID
   */
  deleteImage: async (id) => {
    const response = await axios.delete(`${API_BASE_URL}/${id}`);
    return response.data;
  },

  /**
   * Xóa toàn bộ lịch sử
   */
  clearHistory: async () => {
    const response = await axios.delete(`${API_BASE_URL}/history`);
    return response.data;
  },
};

export default api;
