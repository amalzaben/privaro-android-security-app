package com.privaro.backend.repository;

import com.privaro.backend.entity.ScanLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanLogRepository extends JpaRepository<ScanLog, Long> {

    List<ScanLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<ScanLog> findByUserId(Long userId, Pageable pageable);
}
