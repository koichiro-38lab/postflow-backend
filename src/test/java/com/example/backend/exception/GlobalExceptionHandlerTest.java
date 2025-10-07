package com.example.backend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.hibernate.exception.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    // IllegalArgumentExceptionが発生した場合、400を返すことを確認
    @Test
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");
        var response = globalExceptionHandler.handleIllegalArgument(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("Bad Request", response.getBody().error());
    }

    // 一般的な例外が発生した場合、500を返すことを確認
    @Test
    void handleGeneralException_returns500() {
        Exception ex = new Exception("General error");
        var response = globalExceptionHandler.handleGeneralException(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("Internal Server Error", response.getBody().error());
    }

    // AccessDeniedExceptionの処理が正しく403を返すことを確認
    @Test
    void handleAccessDeniedException_returns403() {
        AccessDeniedException ex = new AccessDeniedException("forbidden");
        var response = globalExceptionHandler.handleAccessDeniedException(ex);

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("Forbidden", response.getBody().error());
    }

    // InvalidEnum値やJSONパースエラーで400を返すことを確認
    @Test
    void handleInvalidEnum_returns400() {
        HttpInputMessage mockInput = mock(HttpInputMessage.class);
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Invalid enum", mockInput);
        var response = globalExceptionHandler.handleInvalidEnum(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("Bad Request", response.getBody().error());
    }

    // DataIntegrityViolationExceptionが一意制約違反の場合、409を返すことを確認
    @Test
    void handleDataIntegrityViolation_returns409ForUniqueViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint");
        var response = globalExceptionHandler.handleDataIntegrityViolation(ex);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("Conflict", response.getBody().error());
    }

    // DataIntegrityViolationExceptionが一意制約違反でない場合、400を返すことを確認
    @Test
    void handleDataIntegrityViolation_returns400ForOtherViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("other violation");
        var response = globalExceptionHandler.handleDataIntegrityViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("Bad Request", response.getBody().error());
    }

    // AccountDisabledExceptionの処理が正しく403を返すことを確認
    @Test
    void handleAccountDisabledException_returns403() {
        AccountDisabledException ex = new AccountDisabledException("Account disabled");
        var response = globalExceptionHandler.handleAccountDisabledException(ex);

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("Forbidden", response.getBody().error());
    }

    // MethodArgumentNotValidExceptionの処理が正しく400を返すことを確認
    @Test
    void handleValidationExceptions_returns400() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "field1", "validation error"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(org.springframework.core.MethodParameter.class), bindingResult);
        var response = globalExceptionHandler.handleValidationExceptions(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("Bad Request", response.getBody().error());
    }

    // IllegalArgumentExceptionが発生した場合、400を返すことを確認
    // SQLStateが23505の場合に一意制約違反を検知することを確認
    @Test
    void isUniqueConstraintViolation_detectsSQLState() {
        java.sql.SQLException sqlEx = new java.sql.SQLException("duplicate", "23505");
        ConstraintViolationException cve = new ConstraintViolationException("duplicate", sqlEx, "23505");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("outer", cve);

        boolean result = (boolean) ReflectionTestUtils.invokeMethod(globalExceptionHandler,
                "isUniqueConstraintViolation", dive);
        assertTrue(result);
    }

    // メッセージ内容に"duplicate key value"が含まれる場合に一意制約違反を検知することを確認
    @Test
    void isUniqueConstraintViolation_detectsMessageContent() {
        java.sql.SQLException sqlEx = new java.sql.SQLException("duplicate key value", "00000");
        ConstraintViolationException cve = new ConstraintViolationException("duplicate key test", sqlEx, "00000");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("outer", cve);

        boolean result = (boolean) ReflectionTestUtils.invokeMethod(globalExceptionHandler,
                "isUniqueConstraintViolation", dive);
        assertTrue(result);
    }

    // ネストされた原因に一意制約違反が含まれる場合を検知することを確認
    @Test
    void isUniqueConstraintViolation_detectsNestedCause() {
        java.sql.SQLException sqlEx = new java.sql.SQLException("duplicate", "23505");
        ConstraintViolationException cve = new ConstraintViolationException("duplicate", sqlEx, "23505");
        RuntimeException intermediate = new RuntimeException("intermediate", cve);
        DataIntegrityViolationException dive = new DataIntegrityViolationException("outer", intermediate);

        boolean result = (boolean) ReflectionTestUtils.invokeMethod(globalExceptionHandler,
                "isUniqueConstraintViolation", dive);
        assertTrue(result);
    }

    // 制約違反がない場合にfalseを返すことを確認
    @Test
    void isUniqueConstraintViolation_returnsFalseWhenNoConstraintViolation() {
        DataIntegrityViolationException dive = new DataIntegrityViolationException("No constraint violation");

        boolean result = (boolean) ReflectionTestUtils.invokeMethod(globalExceptionHandler,
                "isUniqueConstraintViolation", dive);
        assertFalse(result);
    }

    // 異なるSQLStateの場合にfalseを返すことを確認
    @Test
    void isUniqueConstraintViolation_returnsFalseForDifferentSQLState() {
        java.sql.SQLException sqlEx = new java.sql.SQLException("different error", "42000");
        ConstraintViolationException cve = new ConstraintViolationException("different error", sqlEx, "42000");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("outer", cve);

        boolean result = (boolean) ReflectionTestUtils.invokeMethod(globalExceptionHandler,
                "isUniqueConstraintViolation", dive);
        assertFalse(result);
    }

    // 異なるメッセージの場合にfalseを返すことを確認
    @Test
    void isUniqueConstraintViolation_returnsFalseForDifferentMessage() {
        java.sql.SQLException sqlEx = new java.sql.SQLException("different message", "00000");
        ConstraintViolationException cve = new ConstraintViolationException("different message", sqlEx, "00000");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("outer", cve);

        boolean result = (boolean) ReflectionTestUtils.invokeMethod(globalExceptionHandler,
                "isUniqueConstraintViolation", dive);
        assertFalse(result);
    }
}
