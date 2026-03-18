package com.app.entity;

/**
 * Enum đại diện cho trạng thái xử lý của ảnh
 */
public enum ProcessingStatus {
    PENDING, // Đang chờ xử lý
    PROCESSING, // Đang xử lý
    COMPLETED, // Hoàn thành
    FAILED // Thất bại
}
