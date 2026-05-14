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
                    "    <h1 style=\"margin: 0; font-size: 24px;\">Xác Nhận Ứng Tuyển Thành Công</h1>" +
                    "  </div>" +
                    "  <div style=\"padding: 30px;\">" +
                    "    <p>Chào <strong>" + fullName + "</strong>,</p>" +
                    "    <p>Chúc mừng bạn! Bạn đã ứng tuyển thành công vào vị trí:</p>" +
                    "    <div style=\"background-color: #f9fafb; border-left: 4px solid #4f46e5; padding: 15px; margin: 20px 0;\">" +
                    "      <p style=\"margin: 0; font-size: 18px; color: #4f46e5;\"><strong>" + jobTitle + "</strong></p>" +
                    "      <p style=\"margin: 5px 0 0 0; color: #6b7280;\">Tại: " + companyName + "</p>" +
                    "    </div>" +
                    "    <p>Hồ sơ của bạn đã được chuyển đến bộ phận nhân sự của công ty. Chúng tôi sẽ thông báo cho bạn ngay khi có cập nhật mới về trạng thái đơn ứng tuyển.</p>" +
                    "    <p>Bạn có thể theo dõi trạng thái ứng tuyển của mình ngay trên hệ thống <strong>CvAdvisor Platform</strong>.</p>" +
                    "    <p style=\"margin-top: 30px;\">Trân trọng,<br><strong>Đội ngũ CvAdvisor</strong></p>" +
                    "  </div>" +
                    "  <div style=\"background-color: #f3f4f6; color: #9ca3af; padding: 15px; text-align: center; font-size: 12px;\">" +
                    "    <p style=\"margin: 0;\">Đây là email tự động, vui lòng không phản hồi email này.</p>" +
                    "    <p style=\"margin: 5px 0 0 0;\">&copy; 2026 CvAdvisor Platform. All rights reserved.</p>" +
                    "  </div>" +
                    "</div>";

            helper.setTo(toEmail);
            helper.setSubject("Xác nhận ứng tuyển thành công - " + jobTitle);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email xác nhận ứng tuyển đã được gửi thành công đến: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email xác nhận ứng tuyển đến {}: {}", toEmail, e.getMessage());
        }
    }
}
