package com.eduflow.repository.communication;

import com.eduflow.entity.communication.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);

    boolean existsByAnnouncementIdAndUserId(Long announcementId, Long userId);

    List<AnnouncementRead> findByUserId(Long userId);

    @Query("SELECT ar.announcement.id FROM AnnouncementRead ar WHERE ar.user.id = :userId")
    List<Long> findReadAnnouncementIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.status = 'PUBLISHED' " +
            "AND (a.targetType = 'ALL' OR a.targetType = 'PARENTS') " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > CURRENT_TIMESTAMP) " +
            "AND a.id NOT IN (SELECT ar.announcement.id FROM AnnouncementRead ar WHERE ar.user.id = :userId)")
    long countUnreadByUserId(@Param("userId") Long userId);
}
