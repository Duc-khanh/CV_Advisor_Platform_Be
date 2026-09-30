-- ========================================================================
-- DATABASE SCRIPT: CvAdvisorPlatform
-- HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU: MySQL 8.0+
-- ĐƯỢC ĐỒNG BỘ CHUẨN XÁC TỪ CÁC JPA ENTITY TRONG SOURCE CODE
-- ========================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `CvAdvisorPlatform`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `CvAdvisorPlatform`;

-- ========================================================================
-- 1. DROP EXISTING TABLES (IF ANY)
-- ========================================================================
DROP TABLE IF EXISTS `job_application_evaluation`;
DROP TABLE IF EXISTS `job_application`;
DROP TABLE IF EXISTS `job_match`;
DROP TABLE IF EXISTS `job_favorite`;
DROP TABLE IF EXISTS `job_skills`;
DROP TABLE IF EXISTS `job_skill`;
DROP TABLE IF EXISTS `cv_skill`;
DROP TABLE IF EXISTS `cv_evaluation`;
DROP TABLE IF EXISTS `cv`;
DROP TABLE IF EXISTS `article_ratings`;
DROP TABLE IF EXISTS `article_likes`;
DROP TABLE IF EXISTS `article_comments`;
DROP TABLE IF EXISTS `article_bookmarks`;
DROP TABLE IF EXISTS `articles`;
DROP TABLE IF EXISTS `job`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `company`;
DROP TABLE IF EXISTS `industry`;
DROP TABLE IF EXISTS `skill`;
DROP TABLE IF EXISTS `role`;

-- ========================================================================
-- 2. CREATE TABLES
-- ========================================================================

-- Table: role (Mapped from Role.java)
CREATE TABLE `role` (
    `role_id` INT AUTO_INCREMENT PRIMARY KEY,
    `role_name` VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: industry (Mapped from Industry.java)
CREATE TABLE `industry` (
    `industry_id` INT AUTO_INCREMENT PRIMARY KEY,
    `industry_name` VARCHAR(255) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: skill (Mapped from Skill.java)
CREATE TABLE `skill` (
    `skill_id` INT AUTO_INCREMENT PRIMARY KEY,
    `skill_name` VARCHAR(255) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: company (Mapped from Company.java)
CREATE TABLE `company` (
    `company_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `company_name` VARCHAR(255) NOT NULL,
    `industry_id` INT NOT NULL,
    `address` VARCHAR(255) DEFAULT NULL,
    `description` LONGTEXT DEFAULT NULL,
    `logo_url` VARCHAR(255) DEFAULT NULL,
    `website_url` VARCHAR(255) DEFAULT NULL,
    `email` VARCHAR(255) DEFAULT NULL,
    `phone` VARCHAR(50) DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_company_industry` FOREIGN KEY (`industry_id`) REFERENCES `industry` (`industry_id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: users (Mapped from User.java)
CREATE TABLE `users` (
    `user_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `full_name` VARCHAR(255) NOT NULL,
    `email` VARCHAR(255) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `avatar` VARCHAR(255) DEFAULT NULL,
    `role_id` INT NOT NULL,
    `company_id` BIGINT DEFAULT NULL,
    `phone` VARCHAR(50) DEFAULT NULL,
    `headline` VARCHAR(255) DEFAULT NULL,
    `location` VARCHAR(255) DEFAULT NULL,
    `bio` TEXT DEFAULT NULL,
    `birthday` VARCHAR(50) DEFAULT NULL,
    `gender` VARCHAR(50) DEFAULT NULL,
    `personal_link` VARCHAR(255) DEFAULT NULL,
    `skills` TEXT DEFAULT NULL,
    `experience` TEXT DEFAULT NULL,
    `education` TEXT DEFAULT NULL,
    `projects` TEXT DEFAULT NULL,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `hr_approval_status` VARCHAR(20) DEFAULT NULL COMMENT 'PENDING, APPROVED, REJECTED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_users_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON UPDATE CASCADE,
    CONSTRAINT `fk_users_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`company_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: job (Mapped from Job.java)
CREATE TABLE `job` (
    `job_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `company_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `experience_level` VARCHAR(100) DEFAULT NULL,
    `description` LONGTEXT DEFAULT NULL,
    `candidate_requirements` LONGTEXT DEFAULT NULL,
    `location` VARCHAR(255) DEFAULT NULL,
    `salary_range` VARCHAR(100) DEFAULT NULL,
    `job_type` VARCHAR(100) DEFAULT NULL,
    `active` TINYINT(1) DEFAULT 1,
    `vacancies` INT DEFAULT 1,
    `expired_at` DATETIME DEFAULT NULL,
    `image_url` VARCHAR(255) DEFAULT NULL,
    `view_count` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_job_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`company_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: job_skills (Mapped from @ElementCollection in Job.java)
CREATE TABLE `job_skills` (
    `job_id` BIGINT NOT NULL,
    `skill` VARCHAR(255) NOT NULL,
    CONSTRAINT `fk_job_skills_job` FOREIGN KEY (`job_id`) REFERENCES `job` (`job_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: job_skill (Mapped from JobSkill.java - join table with Skill entity)
CREATE TABLE `job_skill` (
    `job_id` BIGINT NOT NULL,
    `skill_id` INT NOT NULL,
    PRIMARY KEY (`job_id`, `skill_id`),
    CONSTRAINT `fk_job_skill_job` FOREIGN KEY (`job_id`) REFERENCES `job` (`job_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_job_skill_skill` FOREIGN KEY (`skill_id`) REFERENCES `skill` (`skill_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: cv (Mapped from CV.java)
CREATE TABLE `cv` (
    `cv_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `file_name` VARCHAR(255) DEFAULT NULL,
    `cv_text` LONGTEXT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_cv_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: cv_skill (Mapped from CvSkill.java)
CREATE TABLE `cv_skill` (
    `cv_id` BIGINT NOT NULL,
    `skill_id` INT NOT NULL,
    PRIMARY KEY (`cv_id`, `skill_id`),
    CONSTRAINT `fk_cv_skill_cv` FOREIGN KEY (`cv_id`) REFERENCES `cv` (`cv_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_cv_skill_skill` FOREIGN KEY (`skill_id`) REFERENCES `skill` (`skill_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: cv_evaluation (Mapped from CVEvaluation.java)
CREATE TABLE `cv_evaluation` (
    `evaluation_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `cv_id` BIGINT NOT NULL,
    `score` INT DEFAULT NULL,
    `strengths` LONGTEXT DEFAULT NULL,
    `weaknesses` LONGTEXT DEFAULT NULL,
    `suggestions` LONGTEXT DEFAULT NULL,
    `evaluated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_cv_eval_cv` FOREIGN KEY (`cv_id`) REFERENCES `cv` (`cv_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: job_match (Mapped from JobMatch.java)
CREATE TABLE `job_match` (
    `match_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `cv_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `match_percent` INT DEFAULT NULL,
    `ai_comment` LONGTEXT DEFAULT NULL,
    `matched_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_job_match_cv_job` (`cv_id`, `job_id`),
    CONSTRAINT `fk_job_match_cv` FOREIGN KEY (`cv_id`) REFERENCES `cv` (`cv_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_job_match_job` FOREIGN KEY (`job_id`) REFERENCES `job` (`job_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: job_favorite (Mapped from JobFavorite.java)
CREATE TABLE `job_favorite` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_job_favorite_user_job` (`user_id`, `job_id`),
    CONSTRAINT `fk_job_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_job_favorite_job` FOREIGN KEY (`job_id`) REFERENCES `job` (`job_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: job_application (Mapped from JobApplication.java)
CREATE TABLE `job_application` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT DEFAULT NULL,
    `job_id` BIGINT DEFAULT NULL,
    `cv_file` VARCHAR(255) DEFAULT NULL,
    `file_size` BIGINT DEFAULT NULL,
    `status` VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING, ACCEPTED, REJECTED',
    `applied_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_job_application_user_job` (`user_id`, `job_id`),
    CONSTRAINT `fk_job_application_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_job_application_job` FOREIGN KEY (`job_id`) REFERENCES `job` (`job_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: job_application_evaluation (Mapped from JobApplicationEvaluation.java)
CREATE TABLE `job_application_evaluation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `application_id` BIGINT NOT NULL UNIQUE,
    `score` INT DEFAULT NULL,
    `summary` TEXT DEFAULT NULL,
    `strengths` TEXT DEFAULT NULL COMMENT 'JSON array',
    `weaknesses` TEXT DEFAULT NULL COMMENT 'JSON array',
    `recommendations` TEXT DEFAULT NULL,
    `evaluated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_job_app_eval_application` FOREIGN KEY (`application_id`) REFERENCES `job_application` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: articles (Mapped from Article.java)
CREATE TABLE `articles` (
    `article_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `content` LONGTEXT DEFAULT NULL,
    `category` VARCHAR(100) NOT NULL,
    `image_url` VARCHAR(255) DEFAULT NULL,
    `read_time` VARCHAR(50) DEFAULT NULL,
    `views_count` INT NOT NULL DEFAULT 0,
    `likes_count` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT, PUBLISHED, HIDDEN',
    `is_pinned` TINYINT(1) NOT NULL DEFAULT 0,
    `author_id` BIGINT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_articles_author` FOREIGN KEY (`author_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: article_bookmarks (Mapped from ArticleBookmark.java)
CREATE TABLE `article_bookmarks` (
    `bookmark_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `article_id` BIGINT NOT NULL,
    UNIQUE KEY `uk_article_bookmark_user_article` (`user_id`, `article_id`),
    CONSTRAINT `fk_article_bookmarks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_article_bookmarks_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: article_comments (Mapped from ArticleComment.java)
CREATE TABLE `article_comments` (
    `comment_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `article_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_article_comments_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_article_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: article_likes (Mapped from ArticleLike.java)
CREATE TABLE `article_likes` (
    `like_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `article_id` BIGINT NOT NULL,
    UNIQUE KEY `uk_article_like_user_article` (`user_id`, `article_id`),
    CONSTRAINT `fk_article_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_article_likes_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: article_ratings (Mapped from ArticleRating.java)
CREATE TABLE `article_ratings` (
    `rating_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `article_id` BIGINT NOT NULL,
    `rating_value` INT NOT NULL COMMENT '1 to 5 stars',
    UNIQUE KEY `uk_article_rating_user_article` (`user_id`, `article_id`),
    CONSTRAINT `fk_article_ratings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_article_ratings_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================================
-- 3. INITIAL SEED DATA
-- Mật khẩu mặc định cho tất cả user mẫu: password123
-- BCrypt Hash: $2a$10$/erU4ZVT3ErkqTDxzGLHCeI17jvS1Ic6Jicl5kT2PeIc4eXTAN.tm
-- ========================================================================

-- Insert Roles
INSERT INTO `role` (`role_id`, `role_name`) VALUES
(1, 'USER'),
(2, 'HR'),
(3, 'ADMIN');

-- Insert Industries
INSERT INTO `industry` (`industry_id`, `industry_name`) VALUES
(1, 'Information Technology'),
(2, 'Financial Services'),
(3, 'Marketing & Advertising'),
(4, 'Healthcare & Pharmacy'),
(5, 'Education & Training');

-- Insert Skills
INSERT INTO `skill` (`skill_id`, `skill_name`) VALUES
(1, 'Java'),
(2, 'Spring Boot'),
(3, 'MySQL'),
(4, 'Hibernate'),
(5, 'Microservices'),
(6, 'ReactJS'),
(7, 'Javascript'),
(8, 'Typescript'),
(9, 'TailwindCSS'),
(10, 'Redux'),
(11, 'NodeJS'),
(12, 'Golang'),
(13, 'Redis'),
(14, 'Kafka'),
(15, 'Docker'),
(16, 'Flutter'),
(17, 'Dart'),
(18, 'AWS'),
(19, 'Kubernetes'),
(20, 'Python'),
(21, 'SQL');

-- Insert Companies
INSERT INTO `company` (`company_id`, `company_name`, `industry_id`, `address`, `description`, `logo_url`, `website_url`, `email`, `phone`, `created_at`) VALUES
(1, 'FPT Software', 1, 'FPT Tower, Phạm Văn Bạch, Cầu Giấy, Hà Nội', 'FPT Software là nhà cung cấp dịch vụ công nghệ thông tin hàng đầu tại Việt Nam và khu vực.', 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=150&h=150&fit=crop', 'https://fpt-software.com', 'recruitment@fsoft.com.vn', '02437689048', NOW()),
(2, 'VNG Corporation', 1, 'Z06 Đường số 13, Tân Thuận Đông, Quận 7, TP. Hồ Chí Minh', 'VNG là doanh nghiệp công nghệ hàng đầu Việt Nam kiến tạo hệ sinh thái internet đa dạng.', 'https://images.unsplash.com/photo-1551434678-e076c223a692?w=150&h=150&fit=crop', 'https://vng.com.vn', 'recruitment@vng.com.vn', '02839623888', NOW()),
(3, 'Viettel Group', 1, 'Lô D26 Khu Đô thị mới Cầu Giấy, Yên Hòa, Cầu Giấy, Hà Nội', 'Tập đoàn Công nghiệp - Viễn thông Quân đội Viettel là doanh nghiệp viễn thông lớn nhất Việt Nam.', 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=150&h=150&fit=crop', 'https://viettel.com.vn', 'careers@viettel.com.vn', '18008098', NOW()),
(4, 'Techcombank', 2, '119 Trần Hưng Đạo, Hoàn Kiếm, Hà Nội', 'Ngân hàng Thương mại Cổ phần Kỹ Thương Việt Nam dẫn đầu về chuyển đổi số ngành tài chính.', 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=150&h=150&fit=crop', 'https://techcombank.com', 'hr@techcombank.com.vn', '1800588822', NOW());

-- Insert Users
-- Passwords are encrypted as BCrypt for 'password123'
INSERT INTO `users` (`user_id`, `full_name`, `email`, `password`, `avatar`, `role_id`, `company_id`, `phone`, `headline`, `location`, `bio`, `enabled`, `hr_approval_status`, `created_at`) VALUES
(1, 'Quản Trị Viên', 'admin@example.com', '$2a$10$/erU4ZVT3ErkqTDxzGLHCeI17jvS1Ic6Jicl5kT2PeIc4eXTAN.tm', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop', 3, NULL, '0901234567', 'System Administrator', 'Hà Nội', 'Quản trị viên hệ thống CV Advisor Platform', 1, NULL, NOW()),
(2, 'Nguyễn Văn A', 'candidate@example.com', '$2a$10$/erU4ZVT3ErkqTDxzGLHCeI17jvS1Ic6Jicl5kT2PeIc4eXTAN.tm', 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=200&h=200&fit=crop', 1, NULL, '0912345678', 'Java / Spring Boot Developer', 'Hà Nội', 'Lập trình viên nhiệt huyết với 2 năm kinh nghiệm làm việc với Java và React.', 1, NULL, NOW()),
(3, 'Trần Thị B', 'hr@fpt.com', '$2a$10$/erU4ZVT3ErkqTDxzGLHCeI17jvS1Ic6Jicl5kT2PeIc4eXTAN.tm', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop', 2, 1, '0923456789', 'Senior HR Specialist @ FPT Software', 'Hà Nội', 'Chuyên viên tuyển dụng nhân sự cấp cao tại FPT Software.', 1, 'APPROVED', NOW()),
(4, 'Phạm Văn C', 'hr@vng.com', '$2a$10$/erU4ZVT3ErkqTDxzGLHCeI17jvS1Ic6Jicl5kT2PeIc4eXTAN.tm', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop', 2, 2, '0934567890', 'Tech Talent Acquisition @ VNG', 'TP. Hồ Chí Minh', 'Phụ trách săn đón nhân tài kỹ thuật tại VNG Corporation.', 1, 'APPROVED', NOW());

-- Insert Jobs
INSERT INTO `job` (`job_id`, `company_id`, `title`, `experience_level`, `description`, `candidate_requirements`, `location`, `salary_range`, `job_type`, `active`, `vacancies`, `expired_at`, `image_url`, `view_count`, `created_at`) VALUES
(1, 1, 'Senior Java Backend Engineer (Spring Boot)', 'Senior', 'Chúng tôi đang tìm kiếm kỹ sư Java Backend cấp cao để phát triển hệ thống tài chính vi mô quy mô lớn. Bạn sẽ thiết kế kiến trúc hệ thống, tối ưu hóa các API RESTful, và dẫn dắt đội ngũ lập trình viên trẻ.', 'Yêu cầu tối thiểu 5 năm kinh nghiệm lập trình Java.\nThành thạo Spring Boot, Spring Security, Hibernate.\nKinh nghiệm làm việc tốt với cơ sở dữ liệu MySQL, Redis.\nTư duy thuật toán tốt, khả năng tối ưu hiệu năng câu lệnh SQL.', 'Cầu Giấy, Hà Nội', '25 - 40 triệu', 'Full-time', 1, 5, DATE_ADD(NOW(), INTERVAL 60 DAY), 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=600&h=400&fit=crop', 120, NOW()),
(2, 1, 'React JS Frontend Developer (Mid-level)', 'Mid', 'Tham gia phát triển các cổng giao tiếp và ứng dụng web quản lý CV/Ứng tuyển thông minh. Xây dựng giao diện Responsive, tối ưu hóa tốc độ tải trang và trải nghiệm người dùng.', 'Từ 2-3 năm kinh nghiệm ReactJS thực tế.\nThành thạo Javascript (ES6+), Typescript, HTML5, CSS3.\nKinh nghiệm với TailwindCSS, Redux Toolkit hoặc Recoil.\nBiết sử dụng Git và quy trình CI/CD cơ bản.', 'Cầu Giấy, Hà Nội', '18 - 28 triệu', 'Full-time', 1, 3, DATE_ADD(NOW(), INTERVAL 60 DAY), 'https://images.unsplash.com/photo-1581291518633-83b4ebd1d83e?w=600&h=400&fit=crop', 85, NOW()),
(3, 2, 'Backend Developer (NodeJS / Golang)', 'Mid', 'Tham gia phát triển hệ thống phân phối nội dung trực tuyến và game portal. Làm việc trực tiếp với hệ thống lượng truy cập siêu khủng, đòi hỏi tốc độ xử lý nhanh và độ trễ cực thấp.', 'Kinh nghiệm từ 2 năm với NodeJS (NestJS/Express) hoặc Golang.\nHiểu biết sâu sắc về OOP, Design Patterns và RESTful APIs.\nKinh nghiệm làm việc với Redis, Kafka, MongoDB.\nCó kiến thức cơ bản về Docker và Kubernetes là một lợi thế.', 'Quận 7, TP. Hồ Chí Minh', '20 - 35 triệu', 'Full-time', 1, 4, DATE_ADD(NOW(), INTERVAL 60 DAY), 'https://images.unsplash.com/photo-1573164713988-8665fc963095?w=600&h=400&fit=crop', 95, NOW()),
(4, 2, 'Mobile Flutter Engineer', 'Senior', 'Thiết kế và phát triển các ứng dụng trên di động trong hệ sinh thái của VNG. Xây dựng và duy trì các thành phần UI mượt mà, đồng thời tối ưu hóa tài nguyên phần cứng trên cả iOS & Android.', 'Tối thiểu 3 năm kinh nghiệm lập trình di động với Flutter.\nThành thạo Dart, hiểu rõ về State Management (Bloc, Provider hoặc Riverpod).\nTừng có ứng dụng phát hành thực tế trên App Store & Google Play.\nTư duy UX/UI di động tốt.', 'Quận 7, TP. Hồ Chí Minh', '25 - 45 triệu', 'Full-time', 1, 2, DATE_ADD(NOW(), INTERVAL 60 DAY), 'https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=600&h=400&fit=crop', 60, NOW()),
(5, 3, 'Cloud DevOps Engineer (AWS / Kubernetes)', 'Senior', 'Chịu trách nhiệm thiết kế, triển khai hạ tầng đám mây và tối ưu luồng CI/CD cho các sản phẩm chuyển đổi số quốc gia của Viettel. Đảm bảo an toàn thông tin và tính sẵn sàng cao (High Availability).', 'Có kinh nghiệm làm việc thực tế với AWS hoặc GCP.\nThành thạo Docker, Kubernetes (EKS, self-hosted).\nKinh nghiệm viết script tự động hóa (Bash, Python) và Ansible/Terraform.\nHiểu biết sâu sắc về bảo mật hệ thống mạng.', 'Cầu Giấy, Hà Nội', '30 - 50 triệu', 'Full-time', 1, 2, DATE_ADD(NOW(), INTERVAL 60 DAY), 'https://images.unsplash.com/photo-1600132806370-bf17e65e942f?w=600&h=400&fit=crop', 110, NOW()),
(6, 4, 'Data Analyst / Business Intelligence', 'Mid', 'Phân tích dữ liệu hành vi người dùng trên ứng dụng ngân hàng số Techcombank Mobile. Thiết kế các dashboard báo cáo trực quan cho ban giám đốc, hỗ trợ ra quyết định kinh doanh dựa trên số liệu.', 'Tốt nghiệp ngành Toán - Tin, Công nghệ thông tin, Thống kê.\nTối thiểu 2 năm kinh nghiệm ở vị trí tương đương.\nThành thạo SQL query, khai thác dữ liệu lớn.\nKinh nghiệm sử dụng Power BI, Tableau hoặc Python (Pandas).', 'Hoàn Kiếm, Hà Nội', '22 - 32 triệu', 'Full-time', 1, 3, DATE_ADD(NOW(), INTERVAL 60 DAY), 'https://images.unsplash.com/photo-1551836022-d5d88e9218df?w=600&h=400&fit=crop', 75, NOW()),
(7, 4, 'Fullstack Java & React Developer', 'Senior', 'Chúng tôi tìm kiếm ứng viên đa năng để tham gia dự án nâng cấp ngân hàng điện tử thế hệ mới. Bạn sẽ làm việc cả phần giao diện mượt mà lẫn phần xử lý logic giao dịch ngân hàng an toàn.', 'Thành thạo cả Java/Spring Boot ở Backend và ReactJS ở Frontend.\nHiểu biết sâu về Spring Security, OAuth2, bảo mật ứng dụng tài chính.\nKinh nghiệm xây dựng và tối ưu RESTful API.\nKhả năng làm việc độc lập tốt và có tư duy làm sản phẩm.', 'Hoàn Kiếm, Hà Nội', '35 - 55 triệu', 'Full-time', 1, 4, DATE_ADD(NOW(), INTERVAL 60 DAY), 'https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=600&h=400&fit=crop', 140, NOW());

-- Insert Job Skills (@ElementCollection)
INSERT INTO `job_skills` (`job_id`, `skill`) VALUES
(1, 'Java'), (1, 'Spring Boot'), (1, 'MySQL'), (1, 'Hibernate'), (1, 'Microservices'),
(2, 'ReactJS'), (2, 'Javascript'), (2, 'Typescript'), (2, 'TailwindCSS'), (2, 'Redux'),
(3, 'NodeJS'), (3, 'Golang'), (3, 'Redis'), (3, 'Kafka'), (3, 'Docker'),
(4, 'Flutter'), (4, 'Dart'), (4, 'iOS'), (4, 'Android'),
(5, 'AWS'), (5, 'Kubernetes'), (5, 'Docker'), (5, 'CI/CD'),
(6, 'SQL'), (6, 'Power BI'), (6, 'Python'),
(7, 'Java'), (7, 'Spring Boot'), (7, 'ReactJS'), (7, 'MySQL');

-- Insert Job Skill mapping
INSERT INTO `job_skill` (`job_id`, `skill_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
(2, 6), (2, 7), (2, 8), (2, 9), (2, 10),
(3, 11), (3, 12), (3, 13), (3, 14), (3, 15),
(4, 16), (4, 17),
(5, 18), (5, 19), (5, 15),
(6, 21), (6, 20),
(7, 1), (7, 2), (7, 6), (7, 3);

-- Insert Articles
INSERT INTO `articles` (`article_id`, `title`, `description`, `content`, `category`, `image_url`, `read_time`, `views_count`, `likes_count`, `status`, `is_pinned`, `author_id`, `created_at`) VALUES
(1, '10 Sai lầm phổ biến khi viết CV IT khiến bạn bị loại ngay vòng gửi xe',
 'Rất nhiều ứng viên sở hữu kỹ năng chuyên môn xuất sắc nhưng lại trượt phỏng vấn chỉ vì trình bày CV sai cách. Khám phá ngay 10 lỗi thường gặp nhất.',
 '# 10 Sai lầm phổ biến khi viết CV IT\n\nViết CV là bước khởi đầu quan trọng trên con đường tìm kiếm việc làm mơ ước...\n\n## 1. Trình bày quá dài dòng hoặc lan man\n- Nhà tuyển dụng chỉ dành từ 6-10 giây để lướt qua một chiếc CV.\n- Hãy tóm gọn thông tin trong 1-2 trang.\n\n## 2. Thiếu số liệu minh chứng cho thành tích\n- Thay vì viết: Tham gia làm dự án backend.\n- Hãy viết: Xây dựng hệ thống REST API phục vụ 100,000 người dùng mỗi ngày, giảm thời gian phản hồi 30%.',
 'Kinh nghiệm viết CV', 'https://images.unsplash.com/photo-1586281380349-632531db7ed4?w=800&h=500&fit=crop', '4 phút đọc', 1250, 142, 'PUBLISHED', 1, 1, NOW()),

(2, 'Bí quyết trả lời câu hỏi: Giới thiệu bản thân khi phỏng vấn',
 'Ấn tượng ban đầu quyết định 80% thành bại của buổi phỏng vấn. Hướng dẫn công thức Elevator Pitch kinh điển giúp bạn tỏa sáng trước nhà tuyển dụng.',
 '# Bí quyết giới thiệu bản thân ấn tượng\n\nCâu hỏi đầu tiên trong bất kỳ buổi phỏng vấn nào hầu như luôn là: Hãy giới thiệu đôi nét về bản thân em...\n\n## 1. Công thức Hiện tại - Quá khứ - Tương lai\n- **Hiện tại**: Vai trò công việc và thế mạnh nổi bật nhất của bạn.\n- **Quá khứ**: Kinh nghiệm và những bài học/dự án lớn đã hoàn thành.\n- **Tương lai**: Vì sao bạn muốn gia nhập công ty này.\n\n## 2. Thời lượng lý tưởng\n- Không nên nói quá 2 phút.',
 'Phỏng vấn', 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=800&h=500&fit=crop', '5 phút đọc', 960, 85, 'PUBLISHED', 0, 1, NOW()),

(3, 'Chiến thuật tìm việc hiệu quả trong thời đại công nghệ số',
 'Làm sao để tiếp cận các cơ hội việc làm ẩn, xây dựng mạng lưới quan hệ và tối ưu hồ sơ tìm việc trực tuyến để các nhà tuyển dụng chủ động săn đón bạn.',
 '# Chiến thuật tìm việc hiệu quả\n\nTìm việc là một quá trình đòi hỏi sự kiên nhẫn và chiến thuật rõ ràng.\n\n## 1. Tận dụng các nền tảng tuyển dụng trực tuyến\n- Cập nhật hồ sơ thường xuyên.\n- Sử dụng bộ lọc thông minh.\n\n## 2. Xây dựng thương hiệu cá nhân\n- Tối ưu hóa hồ sơ LinkedIn hoặc GitHub của bạn.',
 'Tìm việc', 'https://images.unsplash.com/photo-1551836022-d5d88e9218df?w=800&h=500&fit=crop', '6 phút đọc', 740, 64, 'PUBLISHED', 0, 1, NOW());

-- Insert Article Comments
INSERT INTO `article_comments` (`comment_id`, `article_id`, `user_id`, `content`, `created_at`) VALUES
(1, 1, 2, 'Bài viết rất hữu ích! Nhờ sửa theo gợi ý mà em đã nhận được lời mời phỏng vấn đầu tiên.', NOW()),
(2, 2, 2, 'Công thức Elevator Pitch áp dụng thực tế rất trôi chảy, cảm ơn admin!', NOW());

-- Insert Article Likes
INSERT INTO `article_likes` (`like_id`, `user_id`, `article_id`) VALUES
(1, 2, 1),
(2, 2, 2);

-- Insert Article Bookmarks
INSERT INTO `article_bookmarks` (`bookmark_id`, `user_id`, `article_id`) VALUES
(1, 2, 1);

-- Insert Article Ratings
INSERT INTO `article_ratings` (`rating_id`, `user_id`, `article_id`, `rating_value`) VALUES
(1, 2, 1, 5),
(2, 2, 2, 5);

-- Insert Sample CV
INSERT INTO `cv` (`cv_id`, `user_id`, `file_name`, `cv_text`, `created_at`) VALUES
(1, 2, 'NguyenVanA_JavaDeveloper.pdf', 'Nguyễn Văn A\nEmail: candidate@example.com | SĐT: 0912345678\nVị trí: Java / Spring Boot Developer\nKỹ năng: Java, Spring Boot, MySQL, REST API, ReactJS, Git\nKinh nghiệm:\n- 2 năm lập trình Java Backend tại ABC Tech\n- Phát triển RESTful API cho hệ thống E-Commerce\n- Thiết kế và tối ưu cơ sở dữ liệu MySQL\nHọc vấn:\n- Cử nhân Công nghệ Thông tin - Đại học Bách Khoa', NOW());

-- Insert CV Skills
INSERT INTO `cv_skill` (`cv_id`, `skill_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 6);

-- Insert CV Evaluation
INSERT INTO `cv_evaluation` (`evaluation_id`, `cv_id`, `score`, `strengths`, `weaknesses`, `suggestions`, `evaluated_at`) VALUES
(1, 1, 85, 'CV có cấu trúc rõ ràng, kỹ năng cốt lõi về Java và Spring Boot vững vàng, có kinh nghiệm thực tế với MySQL.', 'Chưa nêu rõ các chỉ số đo lường hiệu quả công việc (KPI/Metrics). Cần bổ sung các công nghệ Microservices và Docker.', 'Bổ sung thêm các chứng chỉ liên quan đến Cloud hoặc Java nếu có, đưa thêm link GitHub demo dự án.', NOW());

-- Insert Job Applications
INSERT INTO `job_application` (`id`, `user_id`, `job_id`, `cv_file`, `file_size`, `status`, `applied_at`) VALUES
(1, 2, 1, 'candidate_cv_1.pdf', 1048576, 'PENDING', NOW()),
(2, 2, 2, 'candidate_cv_2.pdf', 1048576, 'ACCEPTED', NOW());

-- Insert Job Application Evaluation
INSERT INTO `job_application_evaluation` (`id`, `application_id`, `score`, `summary`, `strengths`, `weaknesses`, `recommendations`, `evaluated_at`) VALUES
(1, 1, 88, 'Ứng viên có tiềm năng rất tốt với vị trí Java Backend, nền tảng Spring Boot và MySQL phù hợp với yêu cầu của FPT Software.', '["Thành thạo Java và Spring Boot", "Nắm chắc kiến trúc REST API", "Kinh nghiệm thực chiến với hệ thống E-commerce"]', '["Kinh nghiệm về Microservices quy mô lớn còn ít", "Cần cải thiện thêm kỹ năng tiếng Anh chuyên ngành"]', 'Nên mời ứng viên vào vòng phỏng vấn kỹ thuật để đánh giá thêm về tư duy giải quyết vấn đề.', NOW());

-- Insert Job Favorite
INSERT INTO `job_favorite` (`id`, `user_id`, `job_id`, `created_at`) VALUES
(1, 2, 1, NOW()),
(2, 2, 7, NOW());

-- Insert Job Match
INSERT INTO `job_match` (`match_id`, `cv_id`, `job_id`, `match_percent`, `ai_comment`, `matched_at`) VALUES
(1, 1, 1, 85, 'Độ tương thích cao (85%). Ứng viên đáp ứng tốt các yêu cầu chính về Java, Spring Boot và MySQL. Điểm thiếu hụt là số năm kinh nghiệm yêu cầu Senior (5 năm) trong khi ứng viên có 2 năm.', NOW()),
(2, 1, 7, 90, 'Rất phù hợp (90%). CV có cả nền tảng vững về Backend (Java) lẫn Frontend (ReactJS), thích hợp cho vị trí Fullstack Developer.', NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================================================
-- HOÀN TẤT TẠO DATABASE VÀ SEED DATA CHO CVADVISORPLATFORM
-- ========================================================================