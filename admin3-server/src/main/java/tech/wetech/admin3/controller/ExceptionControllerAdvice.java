package tech.wetech.admin3.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.wetech.admin3.common.BusinessException;
import tech.wetech.admin3.common.CommonResultStatus;
import tech.wetech.admin3.common.ResultStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author cjbi
 */
@SecurityRequirement(name = "bearerAuth")
@RestControllerAdvice
public class ExceptionControllerAdvice {

    private final Map<ResultStatus, HttpStatus> codeMap = new HashMap<>() {{
        put(CommonResultStatus.FAIL, HttpStatus.BAD_REQUEST);
        put(CommonResultStatus.PARAM_ERROR, HttpStatus.BAD_REQUEST);
        put(CommonResultStatus.RECORD_NOT_EXIST, HttpStatus.BAD_REQUEST);
        put(CommonResultStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
    }};

    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleDefaultErrorView(Exception ex, HttpServletRequest request) {
        log.error("Handle exception, message={}, requestUrl={}", ex.getMessage(), request.getRequestURI(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("code", CommonResultStatus.SERVER_ERROR.getCode());
        body.put("message", ex.getMessage());
        body.put("success", false);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        Map<String, Object> body = Map.of("code", ex.getStatus().getCode(), "message", ex.getMessage(), "success", false);
        return ResponseEntity.status(codeMap.getOrDefault(ex.getStatus(), HttpStatus.BAD_REQUEST)).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        List<String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());
        Map<String, Object> body = getErrorsMap(fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            ConstraintViolationException e) {
        List<String> errors = e.getConstraintViolations().stream()
                .map(constraintViolation -> constraintViolation.getPropertyPath() + constraintViolation.getMessage())
                .collect(Collectors.toList());
        Map<String, Object> body = getErrorsMap(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private Map<String, Object> getErrorsMap(List<String> fieldErrors) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", CommonResultStatus.PARAM_ERROR.getCode());
        body.put("message", fieldErrors.stream().collect(Collectors.joining(", ")));
        body.put("errors", fieldErrors);
        body.put("success", false);
        return body;
    }


}

