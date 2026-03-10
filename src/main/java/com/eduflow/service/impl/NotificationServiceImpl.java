package com.eduflow.service.impl;

import com.eduflow.dto.response.MessageResponse;
import com.eduflow.entity.academic.Parent;
import com.eduflow.entity.academic.Student;
import com.eduflow.entity.communication.Notification;
import com.eduflow.entity.finance.StudentFeeAssignment;
import com.eduflow.entity.user.User;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.academic.StudentRepository;
import com.eduflow.repository.communication.NotificationRepository;
import com.eduflow.repository.finance.PaymentRepository;
import com.eduflow.repository.finance.StudentFeeAssignmentRepository;
import com.eduflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final StudentFeeAssignmentRepository feeAssignmentRepository;
    private final StudentRepository studentRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public Notification createNotification(User recipient, String title, String message,
                                           Notification.NotificationType type,
                                           Notification.NotificationChannel channel,
                                           String referenceType, Long referenceId) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .channel(channel)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .sentAt(LocalDateTime.now())
                .deliveryStatus(Notification.DeliveryStatus.SENT)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created for user {}: {}", recipient.getEmail(), title);
        return notification;
    }

    @Override
    @Transactional
    public MessageResponse sendOverdueFeesNotifications() {
        List<StudentFeeAssignment> overdueAssignments = feeAssignmentRepository.findOverdueFees();

        if (overdueAssignments.isEmpty()) {
            log.info("No overdue fees found");
            return MessageResponse.success("No overdue fees found. No notifications sent.");
        }

        // Group overdue fees by parent to send consolidated notifications
        Map<User, List<StudentFeeAssignment>> parentAssignments = new HashMap<>();
        int studentsWithoutParent = 0;

        for (StudentFeeAssignment assignment : overdueAssignments) {
            Student student = assignment.getStudent();
            if (student == null) continue;

            Parent parent = student.getParent();
            if (parent == null || parent.getUser() == null) {
                studentsWithoutParent++;
                log.warn("Student {} has no parent assigned, skipping notification",
                        student.getStudentId());
                continue;
            }

            User parentUser = parent.getUser();
            parentAssignments.computeIfAbsent(parentUser, k -> new java.util.ArrayList<>())
                    .add(assignment);
        }

        int notificationsSent = 0;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        for (Map.Entry<User, List<StudentFeeAssignment>> entry : parentAssignments.entrySet()) {
            User parentUser = entry.getKey();
            List<StudentFeeAssignment> assignments = entry.getValue();

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Dear ").append(parentUser.getFullName()).append(",\n\n");
            messageBuilder.append("This is a reminder that the following fees are overdue:\n\n");

            BigDecimal totalOverdue = BigDecimal.ZERO;

            for (StudentFeeAssignment assignment : assignments) {
                Student student = assignment.getStudent();
                BigDecimal balance = assignment.getBalance();
                LocalDate dueDate = assignment.getDueDate();

                messageBuilder.append("• ").append(assignment.getFee().getName())
                        .append(" for ").append(student.getUser().getFullName())
                        .append("\n  Amount Due: ").append(balance)
                        .append("\n  Due Date: ").append(dueDate != null ? dueDate.format(dateFormatter) : "N/A")
                        .append("\n\n");

                totalOverdue = totalOverdue.add(balance);
            }

            messageBuilder.append("Total Overdue Amount: ").append(totalOverdue).append("\n\n");
            messageBuilder.append("Please make the payment at your earliest convenience to avoid any disruption to your child's education.\n\n");
            messageBuilder.append("Thank you,\nEduFlow Administration");

            String title = "Overdue Fees Reminder - " + totalOverdue + " Outstanding";

            createNotification(
                    parentUser,
                    title,
                    messageBuilder.toString(),
                    Notification.NotificationType.PAYMENT_REMINDER,
                    Notification.NotificationChannel.IN_APP,
                    "FEE_ASSIGNMENT",
                    assignments.get(0).getId()
            );

            notificationsSent++;
        }

        String resultMessage = String.format(
                "Sent %d notifications for %d overdue fee assignments. %d students without parents were skipped.",
                notificationsSent, overdueAssignments.size(), studentsWithoutParent
        );

        log.info(resultMessage);
        return MessageResponse.success(resultMessage);
    }

    @Override
    @Transactional
    public MessageResponse sendUpcomingFeesNotifications(int daysBeforeDue) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(daysBeforeDue);

        List<StudentFeeAssignment> upcomingAssignments = feeAssignmentRepository.findAll().stream()
                .filter(a -> a.getDueDate() != null)
                .filter(a -> !a.getDueDate().isBefore(today))
                .filter(a -> !a.getDueDate().isAfter(targetDate))
                .filter(a -> a.getStatus() != StudentFeeAssignment.FeeStatus.PAID &&
                             a.getStatus() != StudentFeeAssignment.FeeStatus.WAIVED)
                .toList();

        if (upcomingAssignments.isEmpty()) {
            log.info("No upcoming fees found within {} days", daysBeforeDue);
            return MessageResponse.success("No upcoming fees found. No notifications sent.");
        }

        Map<User, List<StudentFeeAssignment>> parentAssignments = new HashMap<>();
        int studentsWithoutParent = 0;

        for (StudentFeeAssignment assignment : upcomingAssignments) {
            Student student = assignment.getStudent();
            if (student == null) continue;

            Parent parent = student.getParent();
            if (parent == null || parent.getUser() == null) {
                studentsWithoutParent++;
                continue;
            }

            User parentUser = parent.getUser();
            parentAssignments.computeIfAbsent(parentUser, k -> new java.util.ArrayList<>())
                    .add(assignment);
        }

        int notificationsSent = 0;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        for (Map.Entry<User, List<StudentFeeAssignment>> entry : parentAssignments.entrySet()) {
            User parentUser = entry.getKey();
            List<StudentFeeAssignment> assignments = entry.getValue();

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Dear ").append(parentUser.getFullName()).append(",\n\n");
            messageBuilder.append("This is a friendly reminder about upcoming fee payments:\n\n");

            BigDecimal totalDue = BigDecimal.ZERO;

            for (StudentFeeAssignment assignment : assignments) {
                Student student = assignment.getStudent();
                BigDecimal balance = calculateActualBalance(assignment);
                LocalDate dueDate = assignment.getDueDate();

                if (balance.compareTo(BigDecimal.ZERO) <= 0) continue;

                messageBuilder.append("• ").append(assignment.getFee().getName())
                        .append(" for ").append(student.getUser().getFullName())
                        .append("\n  Amount Due: ").append(balance)
                        .append("\n  Due Date: ").append(dueDate.format(dateFormatter))
                        .append("\n\n");

                totalDue = totalDue.add(balance);
            }

            if (totalDue.compareTo(BigDecimal.ZERO) <= 0) continue;

            messageBuilder.append("Total Amount Due: ").append(totalDue).append("\n\n");
            messageBuilder.append("Please make the payment before the due date.\n\n");
            messageBuilder.append("Thank you,\nEduFlow Administration");

            String title = "Payment Reminder - " + totalDue + " Due Soon";

            createNotification(
                    parentUser,
                    title,
                    messageBuilder.toString(),
                    Notification.NotificationType.PAYMENT_REMINDER,
                    Notification.NotificationChannel.IN_APP,
                    "FEE_ASSIGNMENT",
                    assignments.get(0).getId()
            );

            notificationsSent++;
        }

        String resultMessage = String.format(
                "Sent %d notifications for %d upcoming fee assignments (due within %d days).",
                notificationsSent, upcomingAssignments.size(), daysBeforeDue
        );

        log.info(resultMessage);
        return MessageResponse.success(resultMessage);
    }

    @Override
    @Transactional
    public MessageResponse sendPaymentReminderForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        Parent parent = student.getParent();
        if (parent == null || parent.getUser() == null) {
            return MessageResponse.error("Student does not have a parent assigned");
        }

        List<StudentFeeAssignment> assignments = feeAssignmentRepository.findByStudentId(studentId).stream()
                .filter(a -> a.getStatus() != StudentFeeAssignment.FeeStatus.PAID &&
                             a.getStatus() != StudentFeeAssignment.FeeStatus.WAIVED)
                .toList();

        if (assignments.isEmpty()) {
            return MessageResponse.success("No pending fees for this student. No notification sent.");
        }

        User parentUser = parent.getUser();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Dear ").append(parentUser.getFullName()).append(",\n\n");
        messageBuilder.append("This is a reminder about pending fees for ")
                .append(student.getUser().getFullName()).append(":\n\n");

        BigDecimal totalDue = BigDecimal.ZERO;

        for (StudentFeeAssignment assignment : assignments) {
            BigDecimal balance = calculateActualBalance(assignment);
            LocalDate dueDate = assignment.getDueDate();

            if (balance.compareTo(BigDecimal.ZERO) <= 0) continue;

            messageBuilder.append("• ").append(assignment.getFee().getName())
                    .append("\n  Amount Due: ").append(balance)
                    .append("\n  Due Date: ").append(dueDate != null ? dueDate.format(dateFormatter) : "N/A")
                    .append("\n\n");

            totalDue = totalDue.add(balance);
        }

        if (totalDue.compareTo(BigDecimal.ZERO) <= 0) {
            return MessageResponse.success("All fees are paid for this student. No notification sent.");
        }

        messageBuilder.append("Total Amount Due: ").append(totalDue).append("\n\n");
        messageBuilder.append("Please make the payment at your earliest convenience.\n\n");
        messageBuilder.append("Thank you,\nEduFlow Administration");

        String title = "Payment Reminder for " + student.getUser().getFullName() + " - " + totalDue;

        createNotification(
                parentUser,
                title,
                messageBuilder.toString(),
                Notification.NotificationType.PAYMENT_REMINDER,
                Notification.NotificationChannel.IN_APP,
                "STUDENT",
                studentId
        );

        log.info("Payment reminder sent to {} for student {}", parentUser.getEmail(), student.getStudentId());
        return MessageResponse.success("Payment reminder sent successfully to " + parentUser.getFullName());
    }

    private BigDecimal calculateActualBalance(StudentFeeAssignment assignment) {
        BigDecimal actualPaid = paymentRepository.calculateTotalPaidByFeeAssignmentId(assignment.getId());
        return assignment.getNetAmount().subtract(actualPaid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByRecipientIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByRecipientId(userId);
    }
}
