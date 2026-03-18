import axios from "axios";

// Base URL for API calls
const API_BASE_URL = "/api/images";

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

    return response.data;
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

    return response.data;
  },

  /**
   * Chỉ nhận diện đối tượng (Gemini Vision)
   */
  detectImage: async (file, onProgress) => {
    const formData = new FormData();
    formData.append("file", file);

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

    return response.data;
  },

  /**
   * Lấy thông tin ảnh theo ID
   */
  getImage: async (id) => {
    const response = await axios.get(`${API_BASE_URL}/${id}`);
    return response.data;
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
    return response.data;
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
