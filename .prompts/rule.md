# Development Rules & Guidelines - CvAdvisorPlatform

Tài liệu này định nghĩa các tiêu chuẩn phát triển, kiến trúc, quy tắc coding và quy trình làm việc cho dự án **CvAdvisorPlatform** (Backend: Spring Boot 3.x / Java 17, Frontend: ReactJS). Việc tuân thủ nghiêm ngặt các quy tắc này giúp hệ thống dễ bảo trì, dễ mở rộng, hiệu năng cao và giảm thiểu tối đa bug trong quá trình phát triển sản phẩm thực tế.

---

## 1. Project Overview & Tech Stack
`CvAdvisorPlatform` là nền tảng tuyển dụng thông minh. Hệ thống tự động hóa quá trình kết nối ứng viên và nhà tuyển dụng, nổi bật với khả năng tự động phân tích CV (PDF), đánh giá năng lực ứng viên và gợi ý việc làm thông qua tích hợp Generative AI.

*   **Java Version:** 17 (LTS)
*   **Framework:** Spring Boot 3.4.x (Web, JPA, Security, Mail)
*   **Database:** MySQL 8.x
*   **Security:** JWT (JSON Web Token) & OAuth2 Client (Google Login)
*   **AI Integration:** OpenRouter (Gemini models)
*   **Third-party Services:** Cloudinary (Lưu trữ ảnh), Gmail SMTP (Gửi mail)
*   **File Parser:** Apache PDFBox (Trích xuất văn bản từ CV PDF)

---

## 2. Project Structure & Directory Layout
Dự án tuân thủ cấu trúc phân tầng tiêu chuẩn (Layered Architecture) và phân vùng trách nhiệm rõ r àng:

```text
src/main/java/com/example/cvadvisorplatform/
│
├── config/             # Cấu hình hệ thống (Security, CORS, Mail, WebConfig, Beans...)
├── controller/         # Lớp Controller (Entry point). Phân nhóm theo phân quyền:
│   ├── admin/          # API dành riêng cho quản trị viên (/api/admin/**)
│   ├── hr/             # API dành riêng cho Nhà tuyển dụng (/api/hr/**)
│   ├── users/          # API dành cho ứng viên (/api/user/**)
│   └── (public)        # API không cần xác thực (/api/auth/**, /api/public/**)
│
├── dto/                # Data Transfer Objects (Request/Response Payloads)
├── model/              # JPA Entities đại diện cho các bảng trong DB
├── repository/         # Spring Data JPA Repositories
├── security/           # JWT Filters, UserDetails, Password Encoder và các class liên quan Security
└── service/            # Lớp chứa toàn bộ Business Logic và tích hợp các bên thứ 3 (Gemini, Mail, Cloudinary...)
```

*   **`.prompt/`:** Thư mục root chứa các system prompt tĩnh và template hướng dẫn phân tích của AI. Tuyệt đối không hardcode prompt trong Java code.

---

## 3. Coding Conventions & Standards

### 3.1. Naming Conventions
*   **Classes/Interfaces:** PascalCase (VD: `JobApplicationService`, `UserRepository`).
*   **Methods/Variables:** camelCase (VD: `applyJob()`, `isEmailExists`).
*   **Constants:** UPPER_SNAKE_CASE (VD: `MAX_FILE_SIZE_MB`, `DEFAULT_PAGE_SIZE`).
*   **Database Tables:** Sử dụng snake_case (VD: `users`, `job_applications`, `cv_evaluations`).
*   **Database Columns:** Sử dụng snake_case (VD: `created_at`, `full_name`).

### 3.2. RESTful API Standards
*   **Endpoint URLs:** Sử dụng danh từ số nhiều, chữ thường, cách nhau bởi dấu gạch ngang (kebab-case).
    *   *Đúng:* `/api/v1/jobs`, `/api/v1/cv-documents`
    *   *Sai:* `/api/v1/getJobs`, `/api/v1/cvDocuments`
*   **HTTP Methods:** Sử dụng đúng mục đích:
    *   `GET`: Truy xuất dữ liệu (Không làm thay đổi trạng thái tài nguyên).
    *   `POST`: Tạo mới tài nguyên.
    *   `PUT`: Cập nhật toàn bộ tài nguyên (hoặc thay thế).
    *   `PATCH`: Cập nhật một phần tài nguyên.
    *   `DELETE`: Xóa tài nguyên.
*   **API Versioning:** Bắt buộc sử dụng versioning `/api/v1/...` đối với các API nghiệp vụ chính để đảm bảo khả năng tương thích ngược.

### 3.3. Quy tắc sử dụng Lombok (Rất quan trọng)
*   **JPA Entities:**
    *   **KHÔNG** dùng `@Data` trên các class Entity. `@Data` tự động generate `equals()`, `hashCode()`, và `toString()`. Điều này gây ra lỗi lazy loading (gọi DB liên tục) hoặc lỗi tuần hoàn dữ liệu (Circular Reference) dẫn đến `StackOverflowError` khi thực hiện quan hệ `@OneToMany` / `@ManyToOne`.
    *   **NÊN** sử dụng cụ thể `@Getter` và `@Setter`.
    *   **NÊN** tự viết hoặc sinh `equals()` và `hashCode()` chỉ dựa trên Business Key hoặc Primary Key (`id`), tránh dùng tất cả các trường.
    *   Sử dụng `@ToString(exclude = {"lazyField1", "lazyField2"})` để loại bỏ các trường lazy-loaded khỏi hàm toString.
*   **Constructor Injection:**
    *   Sử dụng `@RequiredArgsConstructor` kết hợp với `private final` cho các fields cần inject. Không dùng `@Autowired` lên field.

### 3.4. Clean Code & Khả năng đọc hiểu
*   **Single Responsibility Principle (SRP):** Mỗi hàm/mỗi class chỉ làm một nhiệm vụ duy nhất.
*   **Hạn chế độ dài:** Một method không nên vượt quá 50 dòng code. Nếu dài hơn, cần tách nhỏ (refactor).
*   **Tránh Nested Loops & Conditions:** Hạn chế dùng lồng nhau quá 3 cấp (Depth < 3). Sử dụng cơ chế return early (trả về sớm) để code phẳng và dễ đọc hơn.
    *   *Ví dụ:* Thay vì `if (user != null) { if (user.isEnabled()) { ... } }`, hãy viết `if (user == null || !user.isEnabled()) return;`.

---

## 4. Architecture & Design Patterns

### 4.1. Luồng dữ liệu (Data Flow)
Dữ liệu chỉ đi theo một chiều: **Client ⇄ Controller ⇄ Service ⇄ Repository ⇄ Database**.
*   **Controller:** Nhận request, validate dữ liệu đầu vào (DTO), gọi Service thích hợp, và format Response. Tuyệt đối không viết business logic ở Controller.
*   **Service:** Xử lý logic nghiệp vụ, tính toán, tương tác với các Service khác hoặc qua Repository.
*   **Repository:** Chỉ làm nhiệm vụ query dữ liệu lên/xuống Database.
*   **Cấm bypass:** Controller tuyệt đối không được gọi trực tiếp Repository để lấy dữ liệu.

### 4.2. Quản lý DTO (Data Transfer Object)
*   Không bao giờ trả trực tiếp Entity của JPA ra ngoài API Client (ReactJS) để bảo mật cấu trúc database và tránh lỗi LazyInitializationException.
*   Mọi Request Body nhận vào và Response Body trả về phải được ánh xạ (map) qua DTO.
*   Sử dụng mapper thủ công (Constructor/Builder) hoặc MapStruct để chuyển đổi giữa Entity và DTO một cách rõ ràng.

### 4.3. Interface-based Programming
*   Đối với các nghiệp vụ phức tạp hoặc có khả năng thay đổi cấu trúc/nhà cung cấp dịch vụ (VD: `GeminiService` / `OpenRouterService`, `FileStorageService`, `MailService`), phải định nghĩa Interface trước, sau đó viết class implementation (VD: `FileStorageServiceImpl`). Điều này hỗ trợ viết Unit Test (mocking) và thay đổi thư viện dễ dàng mà không ảnh hưởng tới code gọi dịch vụ.

---

## 5. Exception Handling & Global Response Standard

### 5.1. Response Standard (Chuẩn hóa phản hồi)
Mọi API trả về cho Frontend phải tuân thủ định dạng JSON thống nhất sau:

#### Khi thành công (Success Response):
```json
{
  "status": "success",
  "code": 200,
  "message": "Thao tác thành công",
  "data": {
    "userId": 12,
    "email": "user@example.com"
  }
}
```

#### Khi thất bại (Error Response):
```json
{
  "status": "error",
  "code": 400,
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": {
    "email": "Email không đúng định dạng",
    "password": "Mật khẩu phải chứa ít nhất 8 ký tự"
  }
}
```

### 5.2. Exception Handling (Xử lý ngoại lệ)
*   **Global Exception Handler:** Bắt buộc xây dựng một class `@RestControllerAdvice` để bắt toàn bộ Exception phát sinh trong hệ thống.
*   **Không ném RuntimeException chung chung:**
    *   Tạo các Custom Exception kế thừa từ `RuntimeException` như `ResourceNotFoundException`, `BadRequestException`, `UnauthorizedException`, `ConflictException`.
    *   Bản thân mỗi Custom Exception sẽ được map với một HTTP Status code tương ứng tại Global Exception Handler.
*   **Không để lộ Stacktrace:** Trong môi trường Production, stacktrace lỗi kỹ thuật phải được log lại bằng Logger và tuyệt đối không trả về trong JSON payload gửi cho client.

---

## 6. Input Validation & Data Safety

### 6.1. Validation
*   Tất cả các trường dữ liệu nhận từ Client thông qua DTO phải được validate chặt chẽ sử dụng các annotation của thư viện `jakarta.validation.constraints` (VD: `@NotBlank`, `@Email`, `@Size`, `@NotNull`, `@Min`, `@Max`).
*   Trong Controller, bắt buộc gắn `@Valid` hoặc `@Validated` trước tham số DTO nhận vào.
    *   *Ví dụ:* `public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request)`

### 6.2. An toàn tải tệp (File Upload Safety)
Vì hệ thống cho phép ứng viên tải CV (PDF) lên, cần áp dụng các quy tắc bảo mật tệp nghiêm ngặt:
1.  **Kiểm tra loại tệp (MIME Type):** Không chỉ kiểm tra đuôi mở rộng `.pdf`, hãy kiểm tra Content-Type của file (`application/pdf`).
2.  **Kiểm tra kích thước (File Size Limit):** Giới hạn tối đa kích thước CV là 5MB (cấu hình trong `application.properties`).
3.  **Lưu trữ an toàn:** Khuyến khích đẩy file lên Cloudinary thông qua `CloudinaryService` thay vì lưu trực tiếp vào thư mục local trong hệ thống để tránh các lỗ hổng thực thi mã độc (Remote Code Execution).
4.  **Sanitize File Name:** Khi lưu trữ cục bộ tạm thời, không sử dụng tên file gốc của người dùng trực tiếp để ghi file. Hãy sinh ra một UUID ngẫu nhiên để đổi tên file nhằm tránh lỗi Path Traversal (`../`).

---

## 7. Security & Authentication Guidelines

*   **Role-based Access Control (RBAC):**
    *   Mọi API nghiệp vụ của ứng dụng phải được phân quyền rõ ràng qua Spring Security.
    *   Cấu hình trong `SecurityFilterChain` hoặc sử dụng Method Security với `@PreAuthorize("hasRole('ROLE_NAME')")` tại lớp Controller.
    *   Frontend chỉ dùng ẩn/hiện UI. Quyền truy cập dữ liệu thực sự phải được kiểm tra (authorize) ở Backend.
*   **Không hardcode Secrets:**
    *   Tất cả khóa bảo mật, mật khẩu email, API key của OpenRouter, Cloudinary, DB credentials phải được lưu ở biến môi trường (`.env` hoặc OS Environment Variables) và tham chiếu qua cú pháp `${KEY_NAME}` trong `application.properties`.
*   **SQL Injection Prevention:**
    *   Luôn dùng Spring Data JPA Query Methods hoặc JPQL với Parameterized Queries (`:param`).
    *   **Cấm** ghép chuỗi thủ công trong các câu lệnh SQL Native để tránh SQL Injection.
*   **CORS (Cross-Origin Resource Sharing):**
    *   Chỉ cấu hình cho phép các Domain cụ thể của Frontend truy cập (thông qua `app.cors.allowed-origins`). Không được dùng `*` ở môi trường Production.

---

## 8. Generative AI & Integration Guidelines (Đặc thù CvAdvisorPlatform)

Vì tính năng cốt lõi của ứng dụng dựa trên kết nối với LLM qua API OpenRouter:
1.  **Xử lý Timeout:** Cuộc gọi đến AI API có độ trễ lớn. Phải cấu hình Timeout rõ ràng cho `RestTemplate` hoặc `WebClient` (ví dụ: Connect Timeout = 10s, Read Timeout = 30s) để tránh treo luồng (thread blocking) của hệ thống.
2.  **Xử lý lỗi ngoại lệ (Fallback & Retry):** Các API bên thứ ba có tỷ lệ lỗi do nghẽn mạng hoặc quá tải (Rate Limit). Bắt buộc phải có khối `try-catch` bọc xung quanh API call, thực hiện retry hoặc trả về một thông báo lỗi thân thiện thay vì làm sập toàn bộ luồng xử lý CV.
3.  **Tách biệt Prompt:** Tránh nhúng các chuỗi Prompt dài ngoằng vào mã Java. Hãy viết và đọc chúng từ các tệp tin trong thư mục `.prompt/` hoặc `prompts/`.
4.  **Kiểm soát Token & Phản hồi:** Đầu ra từ LLM (Gemini) đôi khi không đúng định dạng JSON như mong muốn. Bắt buộc có tầng Parser/Sanitizer để xử lý dữ liệu trả về trước khi convert sang DTO nhằm tránh lỗi `JsonParseException`.

---

## 9. Transaction Management

*   Sử dụng `@Transactional` tại tầng Service cho các hàm có thực hiện ghi, sửa, xóa dữ liệu vào Database.
*   **Read-Only Optimization:** Sử dụng `@Transactional(readOnly = true)` đối với các hàm chỉ đọc dữ liệu để tối ưu hóa hiệu năng Hibernate/JPA.
*   **Lưu ý tự gọi (Self-invocation):** Transaction của Spring được xây dựng trên cơ chế Proxy. Một hàm không có Transaction gọi một hàm có Transaction trong cùng một Class Service sẽ không kích hoạt Transaction. Hãy thiết kế cấu trúc gọi hợp lý.

---

## 10. Logging & Diagnostic Rules

*   Sử dụng annotation `@Slf4j` từ Lombok để ghi log.
*   **CẤM** sử dụng `System.out.println()` và `e.printStackTrace()`.
*   Sử dụng các Level ghi log đúng chuẩn:
    *   `ERROR`: Ghi lại các lỗi hệ thống nghiêm trọng, lỗi kết nối API bên ngoài, hoặc các exception không mong muốn làm gián đoạn luồng xử lý.
    *   `WARN`: Ghi lại các lỗi nghiệp vụ thông thường (Sai mật khẩu, tải lên file quá dung lượng, input không hợp lệ).
    *   `INFO`: Ghi lại các sự kiện vận hành quan trọng (Khởi động hệ thống, User đăng ký thành công, Phân tích xong CV).
    *   `DEBUG`: Dùng trong quá trình phát triển để in ra log chi tiết luồng xử lý, tham số đầu vào của hàm.
*   **Bảo mật thông tin log:** Tuyệt đối không log thông tin nhạy cảm của người dùng (Mật khẩu, JWT token, mã PIN).

---

## 11. Testing & Code Quality

*   **Unit Tests:** Viết Unit Test cho lớp Service xử lý nghiệp vụ chính bằng JUnit 5 và Mockito. Sử dụng mock để cô lập dependencies của Service.
*   **Integration Tests:** Viết Integration Test cho lớp Controller sử dụng `@SpringBootTest` kết hợp `@AutoConfigureMockMvc` để kiểm tra luồng API chạy thực tế từ HTTP Request đến DB.
*   **Code Coverage:** Khuyến khích đạt tối thiểu 70% mức độ phủ sóng kiểm thử (coverage) cho các Service cốt lõi như `AuthService`, `JobApplicationService`, `GeminiService`.

---

## 12. Git & Collaboration Workflow

*   **Branch Naming:**
    *   `main` / `master`: Nhánh production ổn định.
    *   `develop`: Nhánh tích hợp code chính của đội phát triển.
    *   `feature/feature-name`: Nhánh phát triển tính năng mới.
    *   `bugfix/bug-name`: Nhánh sửa lỗi phát sinh.
    *   `hotfix/urgent-bug`: Nhánh sửa lỗi khẩn cấp trực tiếp từ production.
*   **Semantic Commit Messages:**
    *   `feat: <mô tả>`: Thêm tính năng mới.
    *   `fix: <mô tả>`: Sửa lỗi.
    *   `docs: <mô tả>`: Cập nhật tài liệu, comment.
    *   `refactor: <mô tả>`: Tối ưu, cơ cấu lại code nhưng không thay đổi hành vi nghiệp vụ.
    *   `test: <mô tả>`: Thêm hoặc chỉnh sửa unit test.
    *   `chore: <mô tả>`: Cập nhật cấu hình build, dependency, thư viện bổ trợ.