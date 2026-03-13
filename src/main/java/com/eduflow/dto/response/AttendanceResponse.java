package com.eduflow.dto.response;

import com.eduflow.entity.academic.Attendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNumber;
    private Long classId;
    private String className;
    private LocalDate date;
    private Attendance.AttendanceStatus status;
    private String remarks;
    private String markedByName;
}