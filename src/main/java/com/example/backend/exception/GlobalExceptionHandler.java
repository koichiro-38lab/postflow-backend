package com.example.backend.exception;

import com.example.backend.dto.common.ErrorResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // バリデーションエラー
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ErrorResponse.ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ErrorResponse.ValidationError(err.getField(), err.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // BaseException（アプリケーション共通例外）の処理
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        ErrorResponse response = new ErrorResponse(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                List.of(new ErrorResponse.ValidationError("error", ex.getErrorCode())));
        return new ResponseEntity<>(response, ex.getStatus());
    }

    // 不正なEnum値やJSONパースエラー（最小限のハンドラ）
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEnum(HttpMessageNotReadableException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                List.of(new ErrorResponse.ValidationError("error", "error.role.invalid")));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 一意制約違反（例: slug の重複）→ 409 Conflict
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        if (isUniqueConstraintViolation(ex)) {
            ErrorResponse response = new ErrorResponse(
                    HttpStatus.CONFLICT.value(),
                    "Conflict",
                    List.of(new ErrorResponse.ValidationError("resource", "error.resource.duplicate")));
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                List.of(new ErrorResponse.ValidationError("data", "error.data.integrity")));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // サービス層の不正引数（関連リソース未存在など）→ 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                List.of(new ErrorResponse.ValidationError("error", ex.getMessage())));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 権限不足（403）
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                List.of(new ErrorResponse.ValidationError("error", "error.access.denied")));
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    // アカウント無効例外
    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabledException(AccountDisabledException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                List.of(new ErrorResponse.ValidationError("account", "error.account.disabled")));
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    // その他の例外
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                List.of(new ErrorResponse.ValidationError("server", "error.server.internal")));
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 一意制約違反を判定するヘルパーメソッド
    private boolean isUniqueConstraintViolation(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof ConstraintViolationException cve) {
                String state = cve.getSQLState();
                if ("23505".equals(state)) { // PostgreSQL unique_violation
                    return true;
                }
                String msg = cve.getMessage();
                if (msg != null && msg.toLowerCase().contains("duplicate key"))
                    return true;
            }
            String msg = t.getMessage();
            if (msg != null && msg.toLowerCase().contains("duplicate key"))
                return true;
            t = t.getCause();
        }
        return false;
    }
}