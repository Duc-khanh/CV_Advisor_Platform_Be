package com.example.cvadvisorplatform.config;

import com.example.cvadvisorplatform.model.*;
import com.example.cvadvisorplatform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

//@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final IndustryRepository industryRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;
    private final JobApplicationRepository jobApplicationRepository;
    private final ArticleRepository articleRepository;
    private final ArticleCommentRepository articleCommentRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final ArticleBookmarkRepository articleBookmarkRepository;
    private final ArticleRatingRepository articleRatingRepository;

    @org.springframework.beans.factory.annotation.Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(String... args) throws Exception {
        log.info("Starting Database Seeder check...");

        // 1. SEED ROLES
        Role userRole = seedRole("USER");
        Role hrRole = seedRole("HR");
        Role adminRole = seedRole("ADMIN");

        // 2. SEED INDUSTRIES
        Industry itIndustry = seedIndustry("Information Technology");
        Industry financeIndustry = seedIndustry("Financial Services");
        Industry marketingIndustry = seedIndustry("Marketing & Advertising");
        Industry healthcareIndustry = seedIndustry("Healthcare & Pharmacy");

        // 3. SEED COMPANIES
        Company fpt = seedCompany("FPT Software", itIndustry, "FPT Tower, Phạm Văn Bạch, Cầu Giấy, Hà Nội",
                "FPT Software là nhà cung cấp dịch vụ công nghệ thông tin hàng đầu tại Việt Nam và khu vực.",
                "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=150&h=150&fit=crop",
                "https://fpt-software.com", "recruitment@fsoft.com.vn", "02437689048");

        Company vng = seedCompany("VNG Corporation", itIndustry, "Z06 Đường số 13, Tân Thuận Đông, Quận 7, TP. Hồ Chí Minh",
                "VNG là doanh nghiệp công nghệ hàng đầu Việt Nam kiến tạo hệ sinh thái internet đa dạng.",
                "https://images.unsplash.com/photo-1551434678-e076c223a692?w=150&h=150&fit=crop",
                "https://vng.com.vn", "recruitment@vng.com.vn", "02839623888");

        Company viettel = seedCompany("Viettel Group", itIndustry, "Lô D26 Khu đô thị mới Cầu Giấy, Yên Hòa, Cầu Giấy, Hà Nội",
                "Tập đoàn Công nghiệp - Viễn thông Quân đội Viettel là doanh nghiệp viễn thông lớn nhất Việt Nam.",
                "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=150&h=150&fit=crop",
                "https://viettel.com.vn", "careers@viettel.com.vn", "18008098");

        Company techcombank = seedCompany("Techcombank", financeIndustry, "119 Trần Hưng Đạo, Hoàn Kiếm, Hà Nội",
                "Ngân hàng Thương mại Cổ phần Kỹ Thương Việt Nam dẫn đầu về chuyển đổi số ngành tài chính.",
                "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=150&h=150&fit=crop",
                "https://techcombank.com", "hr@techcombank.com.vn", "1800588822");

        // 4. SEED USERS
        User admin = seedUser("Quản Trị Viên", "admin@example.com", "password123", adminRole, null);
        User candidate = seedUser("Nguyễn Văn A", "candidate@example.com", "password123", userRole, null);
        User hrFpt = seedUser("Trần Thị B", "hr@fpt.com", "password123", hrRole, fpt);
        User hrVng = seedUser("Phạm Văn C", "hr@vng.com", "password123", hrRole, vng);

        // 5. SEED JOBS
        if (jobRepository.count() == 0 || jobRepository.findByCompanyCompanyId(fpt.getCompanyId()).isEmpty()) {
            log.info("Seeding FPT Software jobs...");
            // Job 1 (FPT - Java)
            seedJob(fpt, "Senior Java Backend Engineer (Spring Boot)", "Senior",
                    "Chúng tôi đang tìm kiếm kỹ sư Java Backend cấp cao để phát triển hệ thống tài chính vi mô quy mô lớn. Bạn sẽ thiết kế kiến trúc hệ thống, tối ưu hóa các API RESTful, và dẫn dắt đội ngũ lập trình viên trẻ.",
                    "Yêu cầu tối thiểu 5 năm kinh nghiệm lập trình Java.\nThành thạo Spring Boot, Spring Security, Hibernate.\nKinh nghiệm làm việc tốt với cơ sở dữ liệu MySQL, Redis.\nTư duy thuật toán tốt, khả năng tối ưu hiệu năng câu lệnh SQL.",
                    "Cầu Giấy, Hà Nội", "25 - 40 triệu", "Full-time", 5,
                    List.of("Java", "Spring Boot", "MySQL", "Hibernate", "Microservices"),
                    "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=600&h=400&fit=crop");

            // Job 2 (FPT - FrontEnd)
            seedJob(fpt, "React JS Frontend Developer (Mid-level)", "Mid",
                    "Tham gia phát triển các cổng giao tiếp và ứng dụng web quản lý CV/Ứng tuyển thông minh. Xây dựng giao diện Responsive, tối ưu hóa tốc độ tải trang và trải nghiệm người dùng.",
                    "Từ 2-3 năm kinh nghiệm ReactJS thực tế.\nThành thạo Javascript (ES6+), Typescript, HTML5, CSS3.\nKinh nghiệm với TailwindCSS, Redux Toolkit hoặc Recoil.\nBiết sử dụng Git và quy trình CI/CD cơ bản.",
                    "Cầu Giấy, Hà Nội", "18 - 28 triệu", "Full-time", 3,
                    List.of("ReactJS", "Javascript", "Typescript", "TailwindCSS", "Redux"),
                    "https://images.unsplash.com/photo-1581291518633-83b4ebd1d83e?w=600&h=400&fit=crop");
        }

        if (jobRepository.count() == 0 || jobRepository.findByCompanyCompanyId(vng.getCompanyId()).isEmpty()) {
            log.info("Seeding VNG Corporation jobs...");
            // Job 3 (VNG - Golang/NodeJS)
            seedJob(vng, "Backend Developer (NodeJS / Golang)", "Mid",
                    "Tham gia phát triển hệ thống phân phối nội dung trực tuyến và game portal. Làm việc trực tiếp với hệ thống lượng truy cập siêu khủng, đòi hỏi tốc độ xử lý nhanh và độ trễ cực thấp.",
                    "Kinh nghiệm từ 2 năm với NodeJS (NestJS/Express) hoặc Golang.\nHiểu biết sâu sắc về OOP, Design Patterns và RESTful APIs.\nKinh nghiệm làm việc với Redis, Kafka, MongoDB.\nCó kiến thức cơ bản về Docker và Kubernetes là một lợi thế.",
                    "Quận 7, TP. Hồ Chí Minh", "20 - 35 triệu", "Full-time", 4,
                    List.of("NodeJS", "Golang", "Redis", "Kafka", "Docker"),
                    "https://images.unsplash.com/photo-1573164713988-8665fc963095?w=600&h=400&fit=crop");

            // Job 4 (VNG - Mobile Dev)
            seedJob(vng, "Mobile Flutter Engineer", "Senior",
                    "Thiết kế và phát triển các ứng dụng trên di động trong hệ sinh thái của VNG. Xây dựng và duy trì các thành phần UI mượt mà, đồng thời tối ưu hóa tài nguyên phần cứng trên cả iOS & Android.",
                    "Tối thiểu 3 năm kinh nghiệm lập trình di động với Flutter.\nThành thạo Dart, hiểu rõ về State Management (Bloc, Provider hoặc Riverpod).\nTừng có ứng dụng phát hành thực tế trên App Store & Google Play.\nTư duy UX/UI di động tốt.",
                    "Quận 7, TP. Hồ Chí Minh", "25 - 45 triệu", "Full-time", 2,
                    List.of("Flutter", "Dart", "iOS", "Android", "Mobile Development"),
                    "https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=600&h=400&fit=crop");
        }

        if (jobRepository.count() == 0 || jobRepository.findByCompanyCompanyId(viettel.getCompanyId()).isEmpty()) {
            log.info("Seeding Viettel Group jobs...");
            // Job 5 (Viettel - DevOps)
            seedJob(viettel, "Cloud DevOps Engineer (AWS / Kubernetes)", "Senior",
                    "Chịu trách nhiệm thiết kế, triển khai hạ tầng đám mây và tối ưu luồng CI/CD cho các sản phẩm chuyển đổi số quốc gia của Viettel. Đảm bảo an toàn thông tin và tính sẵn sàng cao (High Availability).",
                    "Có kinh nghiệm làm việc thực tế với AWS hoặc GCP.\nThành thạo Docker, Kubernetes (EKS, self-hosted).\nKinh nghiệm viết script tự động hóa (Bash, Python) và Ansible/Terraform.\nHiểu biết sâu sắc về bảo mật hệ thống mạng.",
                    "Cầu Giấy, Hà Nội", "30 - 50 triệu", "Full-time", 2,
                    List.of("AWS", "Kubernetes", "Docker", "Terraform", "CI/CD"),
                    "https://images.unsplash.com/photo-1600132806370-bf17e65e942f?w=600&h=400&fit=crop");
        }

        if (jobRepository.count() == 0 || jobRepository.findByCompanyCompanyId(techcombank.getCompanyId()).isEmpty()) {
            log.info("Seeding Techcombank jobs...");
            // Job 6 (Techcombank - Data Analyst)
            seedJob(techcombank, "Data Analyst / Business Intelligence", "Mid",
                    "Phân tích dữ liệu hành vi người dùng trên ứng dụng ngân hàng số Techcombank Mobile. Thiết kế các dashboard báo cáo trực quan cho ban giám đốc, hỗ trợ ra quyết định kinh doanh dựa trên số liệu.",
                    "Tốt nghiệp ngành Toán - Tin, Công nghệ thông tin, Thống kê.\nTối thiểu 2 năm kinh nghiệm ở vị trí tương đương.\nThành thạo SQL query, khai thác dữ liệu lớn.\nKinh nghiệm sử dụng Power BI, Tableau hoặc Python (Pandas).",
                    "Hoàn Kiếm, Hà Nội", "22 - 32 triệu", "Full-time", 3,
                    List.of("SQL", "Power BI", "Python", "Data Analysis", "Tableau"),
                    "https://images.unsplash.com/photo-1551836022-d5d88e9218df?w=600&h=400&fit=crop");

            // Job 7 (Techcombank - Fullstack)
            seedJob(techcombank, "Fullstack Java & React Developer", "Senior",
                    "Chúng tôi tìm kiếm ứng viên đa năng để tham gia dự án nâng cấp ngân hàng điện tử thế hệ mới. Bạn sẽ làm việc cả phần giao diện mượt mà lẫn phần xử lý logic giao dịch ngân hàng an toàn.",
                    "Thành thạo cả Java/Spring Boot ở Backend và ReactJS ở Frontend.\nHiểu biết sâu về Spring Security, OAuth2, bảo mật ứng dụng tài chính.\nKinh nghiệm xây dựng và tối ưu RESTful API.\nKhả năng làm việc độc lập tốt và có tư duy làm sản phẩm.",
                    "Hoàn Kiếm, Hà Nội", "35 - 55 triệu", "Full-time", 4,
                    List.of("Java", "Spring Boot", "ReactJS", "REST API", "Spring Security"),
                    "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=600&h=400&fit=crop");
        }

        seedJobApplications(candidate);
        seedArticles(admin);
        log.info("Database Seeder completed successfully!");
    }

    private Role seedRole(String name) {
        return roleRepository.findByRoleName(name).orElseGet(() -> {
            Role role = new Role();
            role.setRoleName(name);
            log.info("Seeding role: {}", name);
            return roleRepository.save(role);
        });
    }

    private Industry seedIndustry(String name) {
        return industryRepository.findByIndustryName(name).orElseGet(() -> {
            Industry ind = new Industry();
            ind.setIndustryName(name);
            log.info("Seeding industry: {}", name);
            return industryRepository.save(ind);
        });
    }

    private Company seedCompany(String name, Industry ind, String address, String desc, String logoUrl, String webUrl, String email, String phone) {
        return companyRepository.findAll().stream()
                .filter(c -> c.getCompanyName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    Company c = new Company();
                    c.setCompanyName(name);
                    c.setIndustry(ind);
                    c.setAddress(address);
                    c.setDescription(desc);
                    c.setLogoUrl(logoUrl);
                    c.setWebsiteUrl(webUrl);
                    c.setEmail(email);
                    c.setPhone(phone);
                    log.info("Seeding company: {}", name);
                    return companyRepository.save(c);
                });
    }

    private User seedUser(String fullName, String email, String password, Role role, Company company) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setCompany(company);
            user.setEnabled(true);
            if ("HR".equals(role.getRoleName())) {
                user.setHrApprovalStatus("APPROVED");
            }
            log.info("Seeding user: {}", email);
            return userRepository.save(user);
        } else {
            boolean updated = false;
            // Nếu user đã tồn tại nhưng chưa có company (đặc biệt là HR), gán company cho họ
            if (role.getRoleName().equals("HR") && user.getCompany() == null && company != null) {
                user.setCompany(company);
                updated = true;
                log.info("Updating existing HR user {} with company: {}", email, company.getCompanyName());
            }
            // Set seeded HR users to APPROVED if status is not set
            if ("HR".equals(role.getRoleName()) && user.getHrApprovalStatus() == null) {
                user.setHrApprovalStatus("APPROVED");
                user.setEnabled(true);
                updated = true;
            }
            // Đảm bảo role được cập nhật đúng
            if (user.getRole() == null || !user.getRole().getRoleName().equals(role.getRoleName())) {
                user.setRole(role);
                updated = true;
                log.info("Updating existing user {} with role: {}", email, role.getRoleName());
            }
            if (updated) {
                return userRepository.save(user);
            }
            return user;
        }
    }

    private void seedJob(Company company, String title, String exp, String desc, String requirements, String loc, String salary, String type, Integer vacancies, List<String> skills, String imageUrl) {
        Job j = new Job();
        j.setCompany(company);
        j.setTitle(title);
        j.setExperienceLevel(exp);
        j.setDescription(desc);
        j.setCandidateRequirements(requirements);
        j.setLocation(loc);
        j.setSalaryRange(salary);
        j.setJobType(type);
        j.setVacancies(vacancies);
        j.setExpiredAt(LocalDateTime.now().plusMonths(2));
        j.setRequiredSkills(skills);
        j.setImageUrl(imageUrl);
        j.setActive(true);
        jobRepository.save(j);
        log.info("Seeding job: {}", title);
    }

    private void seedJobApplications(User candidate) {
        if (jobApplicationRepository.findAllByUserWithJobAndCompany(candidate.getUserId()).isEmpty()) {
            log.info("Seeding default job applications for testing...");

            List<Job> allJobs = jobRepository.findAll();
            if (allJobs.isEmpty()) return;

            // Find FPT Job
            Job fptJob = allJobs.stream()
                    .filter(j -> j.getCompany().getCompanyName().equalsIgnoreCase("FPT Software"))
                    .findFirst()
                    .orElse(allJobs.get(0));

            // Create mock CV file
            String fileName = "mock-cv-nguyen-van-a.pdf";
            java.nio.file.Path cvPath = java.nio.file.Paths.get(uploadDir, "cv", fileName);
            createMockPdf(cvPath);

            JobApplication app1 = new JobApplication();
            app1.setUser(candidate);
            app1.setJob(fptJob);
            app1.setCvFile(fileName);
            app1.setFileSize(10240L);
            app1.setStatus("PENDING");
            app1.setAppliedAt(LocalDateTime.now().minusDays(1));
            jobApplicationRepository.save(app1);

            // Find VNG Job
            Job vngJob = allJobs.stream()
                    .filter(j -> j.getCompany().getCompanyName().equalsIgnoreCase("VNG Corporation"))
                    .findFirst()
                    .orElse(null);

            if (vngJob != null) {
                JobApplication app2 = new JobApplication();
                app2.setUser(candidate);
                app2.setJob(vngJob);
                app2.setCvFile(fileName);
                app2.setFileSize(10240L);
                app2.setStatus("PENDING");
                app2.setAppliedAt(LocalDateTime.now().minusHours(5));
                jobApplicationRepository.save(app2);
            }

            log.info("Successfully seeded job applications and mock PDF CV!");
        }
    }

    private void createMockPdf(java.nio.file.Path targetPath) {
        try {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("RESUME - NGUYEN VAN A");
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(0, -30);
                contentStream.showText("Email: candidate@example.com | Phone: 0987654321");
                contentStream.newLineAtOffset(0, -25);
                contentStream.showText("Position: Senior Java Backend Engineer");
                contentStream.newLineAtOffset(0, -30);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("SKILLS: Java, Spring Boot, MySQL, Hibernate, Microservices, Git, Redis.");
                contentStream.newLineAtOffset(0, -30);
                contentStream.showText("EXPERIENCE:");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("- FPT Software: Senior Java Developer (2022 - Present)");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("- VNG Corporation: Java Developer (2020 - 2022)");
                contentStream.endText();
            }
            java.nio.file.Files.createDirectories(targetPath.getParent());
            document.save(targetPath.toFile());
            document.close();
            log.info("Created mock CV PDF file at: {}", targetPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to create mock PDF", e);
        }
    }

    private void seedArticles(User admin) {
        if (articleRepository.count() > 0) {
            boolean hasOldSeeds = articleRepository.findAll().stream()
                .anyMatch(a -> "Java".equals(a.getCategory()) || "React".equals(a.getCategory()) || a.getTitle().contains("IT"));
            if (hasOldSeeds) {
                log.info("Deleting old IT-specific seed articles and related interactions...");
                articleCommentRepository.deleteAll();
                articleLikeRepository.deleteAll();
                articleBookmarkRepository.deleteAll();
                articleRatingRepository.deleteAll();
                articleRepository.deleteAll();
            }
        }

        if (articleRepository.count() == 0) {
            log.info("Seeding handbook articles...");

            Article art1 = new Article();
            art1.setTitle("Bí quyết viết CV chuyên nghiệp chinh phục mọi nhà tuyển dụng");
            art1.setDescription("CV là tấm vé đầu tiên giúp bạn tiếp cận công việc mơ ước. Khám phá ngay cách cấu trúc CV rõ ràng, trình bày kinh nghiệm ấn tượng và tránh những lỗi cơ bản.");
            art1.setContent("# Bí quyết viết CV chuyên nghiệp chinh phục mọi nhà tuyển dụng\n\nMột chiếc CV ấn tượng sẽ giúp bạn nổi bật giữa hàng trăm ứng viên. Hãy cùng điểm qua các quy tắc vàng để thiết kế CV chuẩn chuyên nghiệp.\n\n## 1. Cấu trúc thông tin khoa học\n- **Thông tin liên hệ**: Tên, Số điện thoại, Email chuyên nghiệp, liên kết LinkedIn (nếu có).\n- **Mục tiêu nghề nghiệp**: Tóm tắt ngắn gọn từ 2-3 câu về định hướng bản thân và giá trị có thể mang lại cho công ty.\n- **Kinh nghiệm làm việc**: Liệt kê theo thứ tự thời gian đảo ngược (mới nhất ở trên). Hãy tập trung vào các kết quả đạt được bằng những con số cụ thể.\n- **Học vấn & Chứng chỉ**: Các bằng cấp chính quy và chứng chỉ chuyên môn liên quan trực tiếp đến công việc đang ứng tuyển.\n\n## 2. Làm nổi bật các kỹ năng cốt lõi\n- Chia rõ kỹ năng chuyên môn (Hard Skills) và kỹ năng mềm (Soft Skills).\n- Chỉ liệt kê những kỹ năng thực sự liên quan đến mô tả công việc (Job Description) của nhà tuyển dụng.\n\n## 3. Trình bày sạch sẽ, chuyên nghiệp\n- Sử dụng font chữ dễ đọc (Arial, Times New Roman, Calibri), cỡ chữ 11-12.\n- Giới hạn độ dài CV trong khoảng 1-2 trang.\n- Luôn lưu và gửi file ở định dạng PDF.");
            art1.setCategory("Viết CV");
            art1.setImageUrl("https://images.unsplash.com/photo-1586281380349-632531db7ed4?w=800&h=500&fit=crop");
            art1.setReadTime("8 phút đọc");
            art1.setViewsCount(1250);
            art1.setLikesCount(120);
            art1.setPinned(true);
            art1.setStatus("PUBLISHED");
            art1.setAuthor(admin);
            articleRepository.save(art1);

            Article art2 = new Article();
            art2.setTitle("Top 10 câu hỏi phỏng vấn phổ biến nhất và cách trả lời thông minh");
            art2.setDescription("Chuẩn bị câu trả lời cho các câu hỏi phỏng vấn kinh điển như: Giới thiệu bản thân, Điểm mạnh - Điểm yếu, hay Tại sao chúng tôi nên tuyển dụng bạn.");
            art2.setContent("# Top 10 câu hỏi phỏng vấn phổ biến nhất\n\nĐi phỏng vấn không chỉ là buổi kiểm tra kiến thức, mà còn là cơ hội để bạn chứng minh sự phù hợp với văn hóa doanh nghiệp. Dưới đây là cách trả lời thông minh cho các câu hỏi thường gặp.\n\n## 1. Hãy giới thiệu bản thân bạn?\n- **Sai lầm**: Kể quá chi tiết về sở thích cá nhân hoặc lịch sử gia đình.\n- **Cách trả lời tốt**: Áp dụng công thức Hiện tại - Quá khứ - Tương lai. Tóm tắt ngắn gọn công việc hiện tại, các dấu mốc nổi bật trước đó và lý do bạn ứng tuyển vào vị trí này.\n\n## 2. Điểm yếu lớn nhất của bạn là gì?\n- **Sai lầm**: Trả lời 'Tôi không có điểm yếu nào' hoặc 'Tôi là người quá hoàn hảo'.\n- **Cách trả lời tốt**: Nêu một điểm yếu thật sự nhưng đi kèm với giải pháp hoặc nỗ lực bạn đang thực hiện để cải thiện nó. Ví dụ: 'Tôi từng gặp khó khăn khi quản lý thời gian, nhưng hiện tại tôi đã sử dụng các ứng dụng như Trello để lập kế hoạch chi tiết hơn.'\n\n## 3. Tại sao chúng tôi nên tuyển dụng bạn?\n- **Cách trả lời tốt**: Đối chiếu trực tiếp năng lực và kinh nghiệm của bạn với các yêu cầu cốt lõi trong bảng mô tả công việc, đồng thời khẳng định sự hào hứng với tầm nhìn của công ty.");
            art2.setCategory("Phỏng vấn");
            art2.setImageUrl("https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=800&h=500&fit=crop");
            art2.setReadTime("5 phút đọc");
            art2.setViewsCount(960);
            art2.setLikesCount(85);
            art2.setPinned(false);
            art2.setStatus("PUBLISHED");
            art2.setAuthor(admin);
            articleRepository.save(art2);

            Article art3 = new Article();
            art3.setTitle("Chiến thuật tìm việc hiệu quả trong thời đại công nghệ số");
            art3.setDescription("Làm sao để tiếp cận các cơ hội việc làm ẩn, xây dựng mạng lưới quan hệ và tối ưu hồ sơ tìm việc trực tuyến để các nhà tuyển dụng chủ động săn đón bạn.");
            art3.setContent("# Chiến thuật tìm việc hiệu quả\n\nTìm việc là một quá trình đòi hỏi sự kiên nhẫn và chiến thuật rõ ràng. Dưới đây là các phương pháp giúp bạn gia tăng cơ hội thành công.\n\n## 1. Tận dụng các nền tảng tuyển dụng trực tuyến\n- Cập nhật hồ sơ thường xuyên trên các website tuyển dụng uy tín.\n- Sử dụng các bộ lọc vị trí địa lý, mức lương, và ngành nghề để tìm đúng công việc mong muốn.\n\n## 2. Xây dựng thương hiệu cá nhân trên mạng xã hội nghề nghiệp\n- Tối ưu hóa hồ sơ trực tuyến của bạn với một bức ảnh đại diện chuyên nghiệp, tóm tắt kinh nghiệm đầy đủ và tiêu đề thu hút.\n- Kết nối với những chuyên gia, nhà tuyển dụng trong ngành để học hỏi và nắm bắt cơ hội.\n\n## 3. Theo dõi sát sao và gửi thư cảm ơn\n- Sau khi nộp hồ sơ hoặc phỏng vấn, hãy chủ động gửi email cảm ơn nhà tuyển dụng. Điều này thể hiện sự chuyên nghiệp và mong muốn gắn bó lâu dài của bạn.");
            art3.setCategory("Tìm việc");
            art3.setImageUrl("https://images.unsplash.com/photo-1551836022-d5d88e9218df?w=800&h=500&fit=crop");
            art3.setReadTime("6 phút đọc");
            art3.setViewsCount(740);
            art3.setLikesCount(64);
            art3.setPinned(false);
            art3.setStatus("PUBLISHED");
            art3.setAuthor(admin);
            articleRepository.save(art3);

            Article art4 = new Article();
            art4.setTitle("Bí quyết đàm phán lương hiệu quả khi đi phỏng vấn");
            art4.setDescription("Thương lượng lương luôn là bước nhạy cảm nhất. Làm sao để đàm phán mức thu nhập xứng đáng với năng lực mà không làm mất lòng nhà tuyển dụng?");
            art4.setContent("# Bí quyết đàm phán lương hiệu quả\n\nĐàm phán lương là quyền lợi chính đáng của mọi ứng viên. Hãy chuẩn bị kỹ càng để có một thỏa thuận thu nhập tương xứng nhất.\n\n## 1. Khảo sát mức lương thị trường\n- Hãy nghiên cứu kỹ mức thu nhập trung bình của ngành nghề và cấp bậc tương ứng trước buổi đàm phán.\n- Không nên đưa ra một con số quá cao so với mặt bằng chung, trừ khi bạn có năng lực vượt trội.\n\n## 2. Tập trung vào giá trị đóng góp\n- Khi đề cập đến mức lương, hãy nêu rõ lý do tại sao bạn xứng đáng với con số đó bằng cách liên kết với các thành tựu hoặc khả năng giải quyết vấn đề của doanh nghiệp.\n\n## 3. Thương lượng cả gói phúc lợi\n- Nếu công ty không thể đáp ứng mức lương cứng mong muốn, bạn có thể đề xuất các phúc lợi khác như: chế độ bảo hiểm nâng cao, trợ cấp đi lại, tiền thưởng hiệu suất, hoặc số ngày phép năm.");
            art4.setCategory("Thương lượng lương");
            art4.setImageUrl("https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&h=500&fit=crop");
            art4.setReadTime("4 phút đọc");
            art4.setViewsCount(1430);
            art4.setLikesCount(156);
            art4.setPinned(false);
            art4.setStatus("PUBLISHED");
            art4.setAuthor(admin);
            articleRepository.save(art4);

            Article art5 = new Article();
            art5.setTitle("Phát triển kỹ năng mềm - Chìa khóa thăng tiến sự nghiệp");
            art5.setDescription("Kỹ năng chuyên môn giúp bạn có việc làm, nhưng kỹ năng mềm mới giúp bạn tiến xa. Khám phá các kỹ năng mềm quan trọng nhất nơi công sở.");
            art5.setContent("# Kỹ năng mềm: Chìa khóa thăng tiến sự nghiệp\n\nBên cạnh chuyên môn vững vàng, các kỹ năng tương tác và xử lý tình huống đóng vai trò quyết định đến sự thành bại trong công việc.\n\n## 1. Kỹ năng giao tiếp hiệu quả\n- Lắng nghe chủ động là yếu tố then chốt. Hãy hiểu rõ thông điệp trước khi phản hồi.\n- Trình bày thông tin rõ ràng, súc tích và mạch lạc cả bằng văn bản lẫn lời nói.\n\n## 2. Kỹ năng làm việc nhóm\n- Tôn trọng ý kiến khác biệt và chủ động phối hợp với các thành viên khác để đạt mục tiêu chung.\n- Giải quyết xung đột nội bộ một cách xây dựng và chuyên nghiệp.\n\n## 3. Kỹ năng quản lý thời gian\n- Sử dụng nguyên tắc Eisenhower (Quan trọng và Khẩn cấp) để sắp xếp thứ tự ưu tiên cho công việc hàng ngày.\n- Tránh trì hoãn bằng cách chia nhỏ các tác vụ phức tạp.");
            art5.setCategory("Kỹ năng");
            art5.setImageUrl("https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&h=500&fit=crop");
            art5.setReadTime("5 phút đọc");
            art5.setViewsCount(1180);
            art5.setLikesCount(98);
            art5.setPinned(false);
            art5.setStatus("PUBLISHED");
            art5.setAuthor(admin);
            articleRepository.save(art5);
        }
    }
}
