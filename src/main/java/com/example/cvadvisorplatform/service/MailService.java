package com.example.cvadvisorplatform.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendApplicationConfirmation(String toEmail, String fullName, String jobTitle, String companyName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, 
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
                    StandardCharsets.UTF_8.name());

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;\">" +
                    "  <div style=\"background-color: #4f46e5; color: white; padding: 20px; text-align: center;\">" +
                    "    <h1 style=\"margin: 0; font-size: 24px;\">XÃ¡c Nháº­n á»¨ng Tuyá»ƒn ThÃ nh CÃ´ng</h1>" +
                    "  </div>" +
                    "  <div style=\"padding: 30px;\">" +
                    "    <p>ChÃ o <strong>" + fullName + "</strong>,</p>" +
                    "    <p>ChÃºc má»«ng báº¡n! Báº¡n Ä‘Ã£ á»©ng tuyá»ƒn thÃ nh cÃ´ng vÃ o vá»‹ trÃ­:</p>" +
                    "    <div style=\"background-color: #f9fafb; border-left: 4px solid #4f46e5; padding: 15px; margin: 20px 0;\">" +
                    "      <p style=\"margin: 0; font-size: 18px; color: #4f46e5;\"><strong>" + jobTitle + "</strong></p>" +
                    "      <p style=\"margin: 5px 0 0 0; color: #6b7280;\">Táº¡i: " + companyName + "</p>" +
                    "    </div>" +
                    "    <p>Há»“ sÆ¡ cá»§a báº¡n Ä‘Ã£ Ä‘Æ°á»£c chuyá»ƒn Ä‘áº¿n bá»™ pháº­n nhÃ¢n sá»± cá»§a cÃ´ng ty. ChÃºng tÃ´i sáº½ thÃ´ng bÃ¡o cho báº¡n ngay khi cÃ³ cáº­p nháº­t má»›i vá» tráº¡ng thÃ¡i Ä‘Æ¡n á»©ng tuyá»ƒn.</p>" +
                    "    <p>Báº¡n cÃ³ thá»ƒ theo dÃµi tráº¡ng thÃ¡i á»©ng tuyá»ƒn cá»§a mÃ¬nh ngay trÃªn há»‡ thá»‘ng <strong>CvAdvisor Platform</strong>.</p>" +
                    "    <p style=\"margin-top: 30px;\">TrÃ¢n trá»ng,<br><strong>Äá»™i ngÅ© CvAdvisor</strong></p>" +
                    "  </div>" +
                    "  <div style=\"background-color: #f3f4f6; color: #9ca3af; padding: 15px; text-align: center; font-size: 12px;\">" +
                    "    <p style=\"margin: 0;\">ÄÃ¢y lÃ  email tá»± Ä‘á»™ng, vui lÃ²ng khÃ´ng pháº£n há»“i email nÃ y.</p>" +
                    "    <p style=\"margin: 5px 0 0 0;\">&copy; 2026 CvAdvisor Platform. All rights reserved.</p>" +
                    "  </div>" +
                    "</div>";

            helper.setTo(toEmail);
            helper.setSubject("XÃ¡c nháº­n á»©ng tuyá»ƒn thÃ nh cÃ´ng - " + jobTitle);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email xÃ¡c nháº­n á»©ng tuyá»ƒn Ä‘Ã£ Ä‘Æ°á»£c gá»­i thÃ nh cÃ´ng Ä‘áº¿n: {}", toEmail);
        } catch (Exception e) {
            log.error("Lá»—i khi gá»­i email xÃ¡c nháº­n á»©ng tuyá»ƒn Ä‘áº¿n {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendInterviewInvitation(
            String toEmail,
            String candidateName,
            String jobTitle,
            String companyName,
            String roundName,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime,
            String interviewType,
            String locationOrLink,
            String notes
    ) {
        sendInterviewInvitation(toEmail, candidateName, jobTitle, companyName, roundName, startTime, endTime, interviewType, locationOrLink, null, notes);
    }

    @Async
    public void sendInterviewInvitation(
            String toEmail,
            String candidateName,
            String jobTitle,
            String companyName,
            String roundName,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime,
            String interviewType,
            String locationOrLink,
            String interviewerName,
            String notes
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            java.time.format.DateTimeFormatter fmt =
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

            if (startTime != null && (endTime == null || !endTime.isAfter(startTime))) {
                endTime = startTime.plusMinutes(60);
            }

            String startFormatted = startTime != null ? startTime.format(fmt) : "Chưa xác định";
            String endFormatted   = endTime   != null ? endTime.format(fmt)   : "Chưa xác định";

            boolean isOnline = !"OFFLINE".equalsIgnoreCase(interviewType);
            String typeLabel = isOnline ? "Trực tuyến (Online Meeting)" : "Trực tiếp (Tại văn phòng)";
            String locationLabel = isOnline ? "Link phỏng vấn" : "Địa điểm";

            String rawLink = (locationOrLink != null) ? locationOrLink.trim() : "";
            boolean hasLink = !rawLink.isBlank();

            String linkDisplayHtml;
            if (isOnline) {
                if (hasLink) {
                    String href = (rawLink.startsWith("http://") || rawLink.startsWith("https://"))
                            ? rawLink
                            : "https://" + rawLink;
                    linkDisplayHtml =
                        "<div style=\"margin-top: 6px;\">" +
                        "  <a href=\"" + href + "\" target=\"_blank\" style=\"display: inline-block; background: linear-gradient(135deg, #0ea5e9, #2563eb); color: #ffffff; text-decoration: none; padding: 10px 22px; border-radius: 6px; font-weight: bold; font-size: 14px; margin-bottom: 8px; box-shadow: 0 2px 8px rgba(14,165,233,0.3);\">👉 Tham gia phỏng vấn ngay</a>" +
                        "  <div style=\"font-size: 13px; color: #475569;\">Đường dẫn cuộc họp: <a href=\"" + href + "\" target=\"_blank\" style=\"color: #0284c7; word-break: break-all; font-weight: 600;\">" + rawLink + "</a></div>" +
                        "</div>";
                } else {
                    linkDisplayHtml = "<span style=\"color: #e11d48; font-style: italic;\">Nhà tuyển dụng sẽ gửi link phòng họp trước giờ bắt đầu.</span>";
                }
            } else {
                linkDisplayHtml = hasLink
                        ? "<strong style=\"color: #0f172a;\">" + rawLink + "</strong>"
                        : "<span style=\"color: #64748b;\">Tại văn phòng công ty (HR sẽ liên hệ hướng dẫn chi tiết).</span>";
            }

            String interviewerRow = (interviewerName != null && !interviewerName.isBlank())
                    ? "<tr><td style=\"padding: 8px 0; color: #64748b; width: 140px;\">👤 Người phỏng vấn</td><td style=\"font-weight: 600; color: #1e293b;\">" + interviewerName + "</td></tr>"
                    : "";

            String notesHtml = (notes != null && !notes.isBlank())
                    ? "<div style=\"background: #eff6ff; border-left: 4px solid #2563eb; padding: 14px 18px; border-radius: 0 8px 8px 0; margin: 20px 0;\">" +
                      "  <p style=\"margin: 0; font-size: 13.5px; color: #1e40af; font-weight: 700;\">📝 Lời nhắn từ Nhà tuyển dụng:</p>" +
                      "  <p style=\"margin: 6px 0 0; font-size: 13.5px; color: #334155; line-height: 1.5;\">" + notes + "</p>" +
                      "</div>"
                    : "";

            String htmlContent =
                "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #1e293b; max-width: 620px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; background: #ffffff; box-shadow: 0 4px 20px rgba(0,0,0,0.05);\">" +
                "  <div style=\"background: linear-gradient(135deg, #0ea5e9, #2563eb); color: #ffffff; padding: 26px 24px; text-align: center;\">" +
                "    <h1 style=\"margin: 0; font-size: 22px; font-weight: 800; letter-spacing: 0.5px;\">🎯 THƯ MỜI PHỎNG VẤN</h1>" +
                "    <p style=\"margin: 6px 0 0; font-size: 15px; opacity: 0.95; font-weight: 600;\">" + companyName + "</p>" +
                "  </div>" +
                "  <div style=\"padding: 28px 24px;\">" +
                "    <p style=\"font-size: 15px; margin-top: 0;\">Chào <strong>" + candidateName + "</strong>,</p>" +
                "    <p style=\"font-size: 14.5px; color: #334155;\">Cảm ơn bạn đã quan tâm và ứng tuyển cho vị trí <strong>" + jobTitle + "</strong> tại <strong>" + companyName + "</strong>. Chúng tôi rất ấn tượng với hồ sơ của bạn và trân trọng kính mời bạn tham gia buổi phỏng vấn trực tiếp cùng đại diện công ty.</p>" +
                "    <div style=\"background: #f0f9ff; border-left: 4px solid #0284c7; padding: 18px 20px; margin: 22px 0; border-radius: 0 10px 10px 0;\">" +
                "      <p style=\"margin: 0 0 12px; font-size: 16px; color: #0284c7; font-weight: 800;\">📌 " + (roundName != null ? roundName : "Vòng phỏng vấn tuyển dụng") + "</p>" +
                "      <table style=\"width: 100%; border-collapse: collapse; font-size: 14px;\">" +
                "        <tr><td style=\"padding: 6px 0; color: #64748b; width: 140px;\">⏰ Bắt đầu</td><td style=\"font-weight: 700; color: #0f172a;\">" + startFormatted + "</td></tr>" +
                "        <tr><td style=\"padding: 6px 0; color: #64748b;\">⏱ Kết thúc (dự kiến)</td><td style=\"font-weight: 600; color: #334155;\">" + endFormatted + "</td></tr>" +
                "        <tr><td style=\"padding: 6px 0; color: #64748b;\">📍 Hình thức</td><td style=\"font-weight: 600; color: #0284c7;\">" + typeLabel + "</td></tr>" +
                interviewerRow +
                "        <tr><td style=\"padding: 8px 0; color: #64748b; vertical-align: top;\">🔗 " + locationLabel + "</td><td>" + linkDisplayHtml + "</td></tr>" +
                "      </table>" +
                "    </div>" +
                notesHtml +
                "    <div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 20px 0;\">" +
                "      <p style=\"margin: 0 0 8px; font-size: 13.5px; font-weight: 700; color: #1e293b;\">📌 Lưu ý chuẩn bị cho buổi phỏng vấn:</p>" +
                "      <ul style=\"margin: 0; padding-left: 20px; font-size: 13px; color: #475569; line-height: 1.7;\">" +
                "        <li>Vui lòng có mặt hoặc truy cập link phòng họp trước <strong>5 - 10 phút</strong> để kiểm tra kết nối mạng, micro và camera.</li>" +
                "        <li>Chuẩn bị không gian yên tĩnh, đủ ánh sáng và trang phục lịch sự, chuyên nghiệp.</li>" +
                "        <li>Chuẩn bị sẵn CV, Portfolio hoặc các dự án tiêu biểu để tiện trao đổi trong quá trình phỏng vấn.</li>" +
                "        <li>Nếu bạn có việc đột xuất cần dời lịch hẹn, vui lòng phản hồi email này ít nhất <strong>2 tiếng</strong> trước giờ phỏng vấn.</li>" +
                "      </ul>" +
                "    </div>" +
                "    <p style=\"font-size: 14.5px; color: #334155;\">Chúc bạn có một buổi phỏng vấn thật thành công và tự tin!</p>" +
                "    <p style=\"margin-top: 24px; font-size: 14.5px;\">Trân trọng,<br><strong style=\"color: #0f172a;\">Bộ phận Tuyển dụng & Nhân tài - " + companyName + "</strong></p>" +
                "  </div>" +
                "  <div style=\"background: #f8fafc; color: #94a3b8; padding: 14px 20px; text-align: center; font-size: 12px; border-top: 1px solid #f1f5f9;\">" +
                "    <p style=\"margin: 0;\">© 2026 " + companyName + " • CareerGo</p>" +
                "  </div>" +
                "</div>";

            helper.setTo(toEmail);
            helper.setSubject("Thư mời phỏng vấn: " + jobTitle + " - " + companyName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email mời phỏng vấn đã gửi thành công đến: {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi gửi email mời phỏng vấn đến {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Gửi email thông báo cập nhật trạng thái đơn ứng tuyển (ACCEPTED / REJECTED).
     */
    @Async
    public void sendApplicationStatusUpdate(
            String toEmail,
            String candidateName,
            String jobTitle,
            String companyName,
            String status
    ) {
        if (toEmail == null || toEmail.isBlank()) return;

        try {
            boolean isAccepted = "ACCEPTED".equalsIgnoreCase(status);
            boolean isRejected = "REJECTED".equalsIgnoreCase(status);

            if (!isAccepted && !isRejected) {
                return; // Chỉ gửi email cho trúng tuyển hoặc từ chối
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            String subject;
            String headerTitle;
            String headerGradient;
            String mainMessage;

            if (isAccepted) {
                subject = "Chúc mừng bạn đã trúng tuyển vị trí " + jobTitle + " - " + companyName;
                headerTitle = "🎉 CHÚC MỪNG TRÚNG TUYỂN";
                headerGradient = "linear-gradient(135deg, #059669, #0284c7)";
                mainMessage =
                        "<p style=\"font-size: 15px; margin-top: 0;\">Chào <strong>" + candidateName + "</strong>,</p>" +
                        "<p style=\"font-size: 14.5px; color: #334155; line-height: 1.6;\">Chúng tôi rất vui mừng thông báo rằng bạn đã xuất sắc vượt qua các vòng phỏng vấn và chính thức <strong>trúng tuyển</strong> vào vị trí <strong>" + jobTitle + "</strong> tại <strong>" + companyName + "</strong>.</p>" +
                        "<div style=\"background: #f0fdf4; border-left: 4px solid #16a34a; padding: 16px 20px; margin: 20px 0; border-radius: 0 8px 8px 0;\">" +
                        "  <p style=\"margin: 0; font-size: 15px; color: #15803d; font-weight: 700;\">Vị trí: " + jobTitle + "</p>" +
                        "  <p style=\"margin: 6px 0 0; font-size: 14px; color: #166534;\">Đơn vị tuyển dụng: " + companyName + "</p>" +
                        "</div>" +
                        "<p style=\"font-size: 14px; color: #334155; line-height: 1.6;\">Bộ phận nhân sự sẽ sớm liên hệ trực tiếp với bạn qua email hoặc điện thoại để trao đổi chi tiết về thư mời nhận việc (Offer Letter), mức đãi ngộ và ngày bắt đầu công việc.</p>" +
                        "<p style=\"font-size: 14px; color: #334155; line-height: 1.6;\">Một lần nữa, chúc mừng bạn và chào đón bạn đến với đội ngũ của chúng tôi!</p>";
            } else {
                subject = "Thông báo kết quả ứng tuyển vị trí " + jobTitle + " - " + companyName;
                headerTitle = "THÔNG BÁO VỀ KẾT QUẢ ỨNG TUYỂN";
                headerGradient = "linear-gradient(135deg, #475569, #334155)";
                mainMessage =
                        "<p style=\"font-size: 15px; margin-top: 0;\">Chào <strong>" + candidateName + "</strong>,</p>" +
                        "<p style=\"font-size: 14.5px; color: #334155; line-height: 1.6;\">Lời đầu tiên, chúng tôi xin chân thành cảm ơn bạn đã dành thời gian và sự quan tâm đến vị trí <strong>" + jobTitle + "</strong> tại <strong>" + companyName + "</strong>.</p>" +
                        "<div style=\"background: #f8fafc; border-left: 4px solid #94a3b8; padding: 16px 20px; margin: 20px 0; border-radius: 0 8px 8px 0;\">" +
                        "  <p style=\"margin: 0; font-size: 14px; color: #475569; line-height: 1.6;\">Sau khi xem xét kỹ lưỡng hồ sơ và đối chiếu với các tiêu chí của đợt tuyển dụng hiện tại, chúng tôi rất tiếc phải thông báo rằng chưa thể đồng hành cùng bạn ở vị trí này tại thời điểm hiện tại.</p>" +
                        "</div>" +
                        "<p style=\"font-size: 14px; color: #334155; line-height: 1.6;\">Dữ liệu hồ sơ của bạn vẫn sẽ được lưu trữ trong hệ thống nhân tài của chúng tôi để ưu tiên liên hệ cho các cơ hội phù hợp hơn trong tương lai.</p>" +
                        "<p style=\"font-size: 14px; color: #334155; line-height: 1.6;\">Chúc bạn luôn dồi dào sức khỏe và gặt hái nhiều thành công rực rỡ trên con đường sự nghiệp!</p>";
            }

            String htmlContent =
                    "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #1e293b; max-width: 620px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; background: #ffffff; box-shadow: 0 4px 20px rgba(0,0,0,0.05);\">" +
                    "  <div style=\"background: " + headerGradient + "; color: #ffffff; padding: 24px 24px; text-align: center;\">" +
                    "    <h1 style=\"margin: 0; font-size: 20px; font-weight: 800; letter-spacing: 0.5px;\">" + headerTitle + "</h1>" +
                    "    <p style=\"margin: 6px 0 0; font-size: 14px; opacity: 0.95; font-weight: 600;\">" + companyName + "</p>" +
                    "  </div>" +
                    "  <div style=\"padding: 28px 24px;\">" +
                    mainMessage +
                    "    <p style=\"margin-top: 24px; font-size: 14.5px;\">Trân trọng,<br><strong style=\"color: #0f172a;\">Bộ phận Tuyển dụng & Nhân tài - " + companyName + "</strong></p>" +
                    "  </div>" +
                    "  <div style=\"background: #f8fafc; color: #94a3b8; padding: 14px 20px; text-align: center; font-size: 12px; border-top: 1px solid #f1f5f9;\">" +
                    "    <p style=\"margin: 0;\">© 2026 " + companyName + " • CareerGo</p>" +
                    "  </div>" +
                    "</div>";

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email thông báo trạng thái ứng tuyển ({}) đã gửi đến: {}", status, toEmail);
        } catch (Exception e) {
            log.error("Lỗi gửi email thông báo trạng thái ứng tuyển đến {}: {}", toEmail, e.getMessage());
        }
    }
}

