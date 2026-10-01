package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.NotificationResponse;
import com.example.cvadvisorplatform.model.*;
import com.example.cvadvisorplatform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final InterviewRepository interviewRepository;
    private final PaymentOrderRepository paymentOrderRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    @Override
    @Transactional
    public List<NotificationResponse> getNotifications(Long userId, String type, Boolean unreadOnly) {
        // Đồng bộ các sự kiện thực tế từ hệ thống (Lịch phỏng vấn, Đơn ứng tuyển, Đơn nạp AI)
        syncRealSystemEvents(userId);

        List<Notification> list;
        if (Boolean.TRUE.equals(unreadOnly)) {
            list = notificationRepository.findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        } else if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            list = notificationRepository.findByUser_UserIdAndTypeOrderByCreatedAtDesc(userId, type.toUpperCase());
        } else {
            list = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        }

        return list.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo: " + notificationId));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập thông báo này");
        }

        notification.setRead(true);
        notification = notificationRepository.saveAndFlush(notification);
        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }


    @Override
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo: " + notificationId));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa thông báo này");
        }

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public void clearAll(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    @Override
    @Transactional
    public void createNotification(Long userId, String type, String title, String message, String link, String actionText, Long referenceId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Notification notification = new Notification(user, type, title, message, link, actionText, referenceId);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void syncRealSystemEvents(Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            // 1. Quét lịch phỏng vấn thực tế của user
            List<JobApplication> applications = jobApplicationRepository.findAllByUserWithJobAndCompany(userId);
            if (applications != null && !applications.isEmpty()) {
                List<Long> appIds = applications.stream().map(JobApplication::getId).collect(Collectors.toList());
                List<Interview> interviews = interviewRepository.findByApplication_IdIn(appIds);

                for (Interview iv : interviews) {
                    boolean exists = notificationRepository.existsByUser_UserIdAndTypeAndReferenceId(userId, "INTERVIEW", iv.getId());
                    if (!exists) {
                        String jobTitle = (iv.getApplication() != null && iv.getApplication().getJob() != null)
                                ? iv.getApplication().getJob().getTitle() : "vị trí ứng tuyển";
                        String companyName = (iv.getCompany() != null) ? iv.getCompany().getCompanyName() : "Nhà tuyển dụng";
                        String timeStr = iv.getStartTime() != null ? iv.getStartTime().format(DATE_TIME_FORMATTER) : "sớm nhất";
                        String roundName = iv.getRoundName() != null ? iv.getRoundName() : "Vòng phỏng vấn";

                        String title = "Lịch phỏng vấn mới: " + roundName;
                        String message = String.format("Bạn có lịch phỏng vấn cho vị trí '%s' tại %s vào lúc %s. Địa điểm/Link: %s",
                                jobTitle, companyName, timeStr, (iv.getLocationOrLink() != null ? iv.getLocationOrLink() : "Xem chi tiết"));

                        Notification notif = new Notification(
                                user,
                                "INTERVIEW",
                                title,
                                message,
                                "/profile?tab=interviews",
                                "Xem lịch phỏng vấn",
                                iv.getId()
                        );
                        notificationRepository.save(notif);
                    }
                }

                // 2. Quét trạng thái hồ sơ ứng tuyển
                for (JobApplication app : applications) {
                    if (app.getStatus() != null && !"PENDING".equalsIgnoreCase(app.getStatus())) {
                        boolean exists = notificationRepository.existsByUser_UserIdAndTypeAndReferenceId(userId, "APPLICATION", app.getId());
                        if (!exists) {
                            String jobTitle = (app.getJob() != null) ? app.getJob().getTitle() : "vị trí ứng tuyển";
                            String companyName = (app.getJob() != null && app.getJob().getCompany() != null)
                                    ? app.getJob().getCompany().getCompanyName() : "Doanh nghiệp";

                            String statusText;
                            switch (app.getStatus().toUpperCase()) {
                                case "ACCEPTED":
                                case "APPROVED":
                                    statusText = "đã được DUYỆT & CHẤP NHẬN";
                                    break;
                                case "REJECTED":
                                    statusText = "đã được nhà tuyển dụng phản hồi";
                                    break;
                                case "REVIEWING":
                                    statusText = "đang được nhà tuyển dụng XEM XÉT";
                                    break;
                                default:
                                    statusText = "đã được cập nhật thành '" + app.getStatus() + "'";
                                    break;
                            }

                            String title = "Cập nhật trạng thái ứng tuyển: " + jobTitle;
                            String message = String.format("Hồ sơ ứng tuyển của bạn cho vị trí '%s' tại %s %s.",
                                    jobTitle, companyName, statusText);

                            Notification notif = new Notification(
                                    user,
                                    "APPLICATION",
                                    title,
                                    message,
                                    "/profile?tab=my_jobs",
                                    "Kiểm tra đơn ứng tuyển",
                                    app.getId()
                            );
                            notificationRepository.save(notif);
                        }
                    }
                }
            }

            // 3. Thông báo chào mừng / hướng dẫn hệ thống ban đầu nếu chưa có thông báo nào
            List<Notification> existing = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
            if (existing == null || existing.isEmpty()) {
                Notification welcomeNotif = new Notification(
                        user,
                        "SYSTEM",
                        "Chào mừng bạn đến với CareerGo!",
                        "Hồ sơ của bạn đã sẵn sàng. Hãy khám phá hàng trăm cơ hội việc làm IT hấp dẫn và sử dụng Trợ lý AI để tối ưu hóa CV của bạn.",
                        "/profile?tab=profile_itviec",
                        "Cập nhật hồ sơ",
                        0L
                );
                notificationRepository.save(welcomeNotif);

                Notification aiNotif = new Notification(
                        user,
                        "AI_USAGE",
                        "Kích hoạt hạn mức AI phân tích CV",
                        "Bạn có lượt sử dụng AI miễn phí để phân tích và chấm điểm độ tương thích CV với mọi tin tuyển dụng.",
                        "/profile?tab=ai_usage",
                        "Xem hạn mức AI",
                        0L
                );
                notificationRepository.save(aiNotif);
            }

        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ sự kiện thông báo hệ thống cho user {}: {}", userId, e.getMessage());
        }
    }
}
