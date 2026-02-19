package com.eduflow.repository.communication;

import com.eduflow.entity.communication.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findByStatusOrderByPublishedAtDesc(Announcement.AnnouncementStatus status, Pageable pageable);

    List<Announcement> findBySenderId(Long senderId);

    @Query("SELECT a FROM Announcement a WHERE a.status = 'PUBLISHED' " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > CURRENT_TIMESTAMP) " +
            "ORDER BY a.priority DESC, a.publishedAt DESC")
    Page<Announcement> findActiveAnnouncements(Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.status = 'PUBLISHED' " +
            "AND (a.targetType = 'ALL' OR a.targetType = :targetType) " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > CURRENT_TIMESTAMP)")
    Page<Announcement> findByTargetType(@Param("targetType") Announcement.TargetType targetType, Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.status = 'SCHEDULED' " +
            "AND a.scheduledAt <= :now")
    List<Announcement> findScheduledToPublish(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Announcement a WHERE a.status = 'PUBLISHED' AND a.expiresAt < :now")
    List<Announcement> findExpiredAnnouncements(@Param("now") LocalDateTime now);
}
