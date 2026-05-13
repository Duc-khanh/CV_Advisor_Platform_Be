# Cấu hình Gemini API cho CvAdvisorPlatform

- **Model**: `gemini-1.5-flash` (Ưu tiên tốc độ xử lý CV nhanh)
- **Temperature**: `0.4` (Giữ cho nhận xét ổn định, tránh sáng tạo quá mức trong đánh giá kỹ thuật)
- **Top_P**: `0.9`
- **Max_Output_Tokens**: `4096`
- **Safety_Settings**:
    - Hate Speech: Block Low
    - Harassment: Block Low