package com.app.repository;

import com.app.entity.ImageTask;
import com.app.entity.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository cho ImageTask entity
 * Cung cấp các phương thức truy vấn database
 */
@Repository
public interface ImageTaskRepository extends JpaRepository<ImageTask, Long> {

    // Tìm theo trạng thái
    List<ImageTask> findByProcessingStatus(ProcessingStatus status);

    // Tìm theo trạng thái với phân trang
    Page<ImageTask> findByProcessingStatus(ProcessingStatus status, Pageable pageable);

    // Tìm tất cả, sắp xếp theo ngày tạo mới nhất
    Page<ImageTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Tìm theo khoảng thời gian
    List<ImageTask> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Đếm theo trạng thái
    long countByProcessingStatus(ProcessingStatus status);

    // === Statistics Queries ===

    // Tổng dung lượng file đã xử lý
    @Query("SELECT COALESCE(SUM(t.fileSize), 0) FROM ImageTask t")
    long sumTotalFileSize();

    // Thời gian xử lý trung bình (chỉ tính task hoàn thành)
    @Query("SELECT COALESCE(AVG(t.processingTimeMs), 0) FROM ImageTask t WHERE t.processingStatus = 'COMPLETED'")
    double avgProcessingTime();

    // Tìm kiếm theo tên file (phân trang)
    @Query("SELECT t FROM ImageTask t WHERE LOWER(t.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY t.createdAt DESC")
    Page<ImageTask> searchByFilename(@Param("keyword") String keyword, Pageable pageable);

    // Đếm ảnh xử lý hôm nay
    @Query("SELECT COUNT(t) FROM ImageTask t WHERE t.createdAt >= :startOfDay")
    long countTodayTasks(@Param("startOfDay") LocalDateTime startOfDay);
}
