# Kỹ năng Chuyên gia CV (CV Specialist Skills)

## Skill: Analyze_Professional_Competencies
- **Mô tả**: Tự động nhận diện lĩnh vực nghề nghiệp, trích xuất và phân tích năng lực chuyên môn từ nội dung CV.
- **Đầu vào**: `raw_text` (nội dung CV).
- **Đầu ra**: Danh sách JSON gồm:
  - `Industry/Domain` (Lĩnh vực ngành nghề)
  - `Core_Competencies` (Kỹ năng chuyên môn lõi)
  - `Softwares_And_Tools` (Phần mềm, hệ thống hoặc công cụ sử dụng)
  - `Certifications_And_Languages` (Chứng chỉ chuyên môn và Ngoại ngữ).

## Skill: Match_Job_Description
- **Mô tả**: So sánh đối chiếu đa chiều giữa năng lực trong CV với yêu cầu của bản mô tả công việc (JD).
- **Đầu vào**: `cv_content`, `jd_content`.
- **Đầu ra**: Điểm phần trăm phù hợp tổng thể (%), điểm mạnh tương thích, và danh sách các kỹ năng/yêu cầu còn thiếu hụt so với JD.

## Skill: Optimize_Professional_Terminology
- **Mô tả**: Kiểm tra văn phong, lỗi ngữ pháp và độ chuẩn xác của các thuật ngữ chuyên môn tương ứng với đúng ngành nghề của ứng viên.
- **Đầu vào**: `cv_content`, `industry_domain`.
- **Đầu ra**: Các lỗi ngữ pháp cần sửa, đánh giá mức độ chuyên nghiệp của văn phong và đề xuất cách dùng từ vựng/thuật ngữ chuẩn xác hơn theo ngành.
