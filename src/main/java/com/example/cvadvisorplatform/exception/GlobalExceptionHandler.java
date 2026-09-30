package com.example.cvadvisorplatform.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> {
                    String defaultMsg = error.getDefaultMessage();
                    if (defaultMsg != null && !defaultMsg.isBlank()) {
                        return defaultMsg;
                    }
                    return error.getField() + " không hợp lệ.";
                })
                .orElse("Dữ liệu nhập vào không hợp lệ.");
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message", message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Đăng nhập thất bại (Bad credentials): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "BAD_CREDENTIALS", "message", "Email hoặc mật khẩu không chính xác."));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFound(UsernameNotFoundException ex) {
        log.warn("Không tìm thấy user: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "USER_NOT_FOUND", "message", "Email hoặc mật khẩu không chính xác."));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<?> handleDisabled(DisabledException ex) {
        log.warn("Tài khoản chưa kích hoạt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "ACCOUNT_DISABLED", "message", "Tài khoản của bạn chưa được kích hoạt hoặc đang chờ duyệt."));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<?> handleLocked(LockedException ex) {
        log.warn("Tài khoản bị khóa: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "ACCOUNT_LOCKED", "message", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Từ chối truy cập: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "FORBIDDEN", "message", "Bạn không có quyền thực hiện thao tác này."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File quá kích thước: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "FILE_TOO_LARGE", "message", "Kích thước tệp tải lên vượt quá giới hạn 5MB cho phép."));
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<?> handleAiProviderException(AiProviderException ex) {
        log.error("AI provider error", ex);
        boolean quotaExceeded = ex.getMessage() != null && ex.getMessage().contains("hết hạn mức");
        return ResponseEntity.status(quotaExceeded ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "code", quotaExceeded ? "AI_QUOTA_EXCEEDED" : "AI_PROVIDER_UNAVAILABLE",
                        "message", ex.getMessage() == null
                                ? "AI tạm thời không phản hồi. Vui lòng thử lại sau."
                                : ex.getMessage()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.error("Database constraint violation: {}", cause, ex);

        String normalizedCause = cause == null ? "" : cause.toLowerCase();
        String message;
        if (normalizedCause.contains("job_application")
                || (normalizedCause.contains("user_id") && normalizedCause.contains("job_id"))
                || normalizedCause.contains("uk_job_application")) {
            message = "Bạn đã ứng tuyển công việc này rồi.";
        } else if (normalizedCause.contains("email")) {
            message = "Email này đã tồn tại trong hệ thống.";
        } else if (normalizedCause.contains("favorite") || normalizedCause.contains("job_favorite")) {
            message = "Công việc đã có trong danh sách việc làm yêu thích.";
        } else {
            message = "Dữ liệu đã tồn tại hoặc không thể lưu. Vui lòng kiểm tra lại.";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "DATA_INTEGRITY_VIOLATION", "message", message));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleIllegalArgOrState(RuntimeException ex) {
        log.warn("Invalid argument or state: {}", ex.getMessage());
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Yêu cầu không hợp lệ.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "BAD_REQUEST", "message", message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        log.warn("Business / Runtime error: {}", ex.getMessage());
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Yêu cầu không hợp lệ. Vui lòng thử lại.";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "BAD_REQUEST", "message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception ex) {
        log.error("Unexpected server error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", "INTERNAL_ERROR", "message", "Hệ thống đang gặp sự cố. Vui lòng thử lại sau."));
    }
}
