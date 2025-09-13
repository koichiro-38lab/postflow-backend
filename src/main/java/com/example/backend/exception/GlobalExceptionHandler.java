package com.example.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.backend.dto.common.ErrorResponse;

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

    // 重複メールアドレス
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DataIntegrityViolationException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                List.of(new ErrorResponse.ValidationError("email", "error.email.duplicate")));
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // ユーザー未発見
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                List.of(new ErrorResponse.ValidationError("id", "error.user.not_found")));
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // 不正なEnum値
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEnum(HttpMessageNotReadableException ex) {
        // role の変換エラーの場合だけ特別対応
        if (ex.getMessage() != null && ex.getMessage().contains("User$Role")) {
            ErrorResponse response = new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Bad Request",
                    List.of(new ErrorResponse.ValidationError("role", "error.role.invalid")));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // その他のJSONパースエラーはまとめて処理
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                List.of(new ErrorResponse.ValidationError("request", "error.request.invalid")));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 重複メールアドレス（サービス層でのチェック用）
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                List.of(new ErrorResponse.ValidationError("email", "error.email.duplicate")));
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
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
}