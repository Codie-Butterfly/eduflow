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

    // Find announcements for parents - includes ALL, PARENTS, and CLASS-targeted for their children's classes
    @Query("SELECT DISTINCT a FROM Announcement a WHERE a.status = 'PUBLISHED' " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > CURRENT_TIMESTAMP) " +
            "AND (a.targetType = 'ALL' OR a.targetType = 'PARENTS' " +
            "OR (a.targetType = 'CLASS' AND EXISTS (SELECT 1 FROM a.targetIds t WHERE t IN :classIds)) " +
            "OR (a.targetType = 'SPECIFIC_USERS' AND EXISTS (SELECT 1 FROM a.targetIds t WHERE t = :userId))) " +
            "ORDER BY a.priority DESC, a.publishedAt DESC")
    Page<Announcement> findAnnouncementsForParent(@Param("classIds") List<Long> classIds,
                                                   @Param("userId") Long userId,
                                                   Pageable pageable);

    // Find announcements for teachers
    @Query("SELECT DISTINCT a FROM Announcement a WHERE a.status = 'PUBLISHED' " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > CURRENT_TIMESTAMP) " +
            "AND (a.targetType = 'ALL' OR a.targetType = 'TEACHERS' " +
            "OR (a.targetType = 'SPECIFIC_USERS' AND EXISTS (SELECT 1 FROM a.targetIds t WHERE t = :userId))) " +
            "ORDER BY a.priority DESC, a.publishedAt DESC")
    Page<Announcement> findAnnouncementsForTeacher(@Param("userId") Long userId, Pageable pageable);

    // Count unread for parent
    @Query("SELECT COUNT(DISTINCT a) FROM Announcement a WHERE a.status = 'PUBLISHED' " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > CURRENT_TIMESTAMP) " +
            "AND (a.targetType = 'ALL' OR a.targetType = 'PARENTS' " +
            "OR (a.targetType = 'CLASS' AND EXISTS (SELECT 1 FROM a.targetIds t WHERE t IN :classIds)) " +
            "OR (a.targetType = 'SPECIFIC_USERS' AND EXISTS (SELECT 1 FROM a.targetIds t WHERE t = :userId))) " +
            "AND a.id NOT IN (SELECT ar.announcement.id FROM AnnouncementRead ar WHERE ar.user.id = :userId)")
    long countUnreadForParent(@Param("classIds") List<Long> classIds, @Param("userId") Long userId);
}
