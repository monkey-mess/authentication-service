package ru.balybin.monkey_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorDetail> userExceptionHandler(UserException e, WebRequest req){
        ErrorDetail errorDetail = new ErrorDetail(e.getMessage(),
                req.getDescription(false), LocalDateTime.now());
        return new ResponseEntity<>(errorDetail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MessageException.class)
    public ResponseEntity<ErrorDetail> messageExceptionHandler(UserException e, WebRequest req){
        ErrorDetail errorDetail = new ErrorDetail(e.getMessage(),
                req.getDescription(false), LocalDateTime.now());
        return new ResponseEntity<>(errorDetail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetail> methodArgumentNotValidExceptionHandler(
            MethodArgumentNotValidException e, WebRequest req){
        String error = e.getBindingResult().getFieldError().getDefaultMessage();
        ErrorDetail errorDetail = new ErrorDetail("VALIDATION_ERROR",error,LocalDateTime.now());
        return new ResponseEntity<>(errorDetail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorDetail> NoHandlerFoundExceptionHandler(NoHandlerFoundException e, WebRequest req){
        ErrorDetail errorDetail = new ErrorDetail("ENDPOINT_NOT_FOUND", e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(errorDetail, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetail> otherExceptionHandler(Exception e, WebRequest req) {
        ErrorDetail errorDetail = new ErrorDetail("INTERNAL_ERROR",
                "An unexpected error occurred", LocalDateTime.now());
        return new ResponseEntity<>(errorDetail, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDetail> badCredentialsExceptionHandler(BadCredentialsException e, WebRequest req) {
        ErrorDetail errorDetail = new ErrorDetail("AUTH_ERROR", "Invalid email or password",
                LocalDateTime.now());
        return new ResponseEntity<>(errorDetail, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDetail> authenticationExceptionHandler(
            AuthenticationException e, WebRequest req) {
        ErrorDetail errorDetail = new ErrorDetail("AUTH_ERROR", e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(errorDetail, HttpStatus.UNAUTHORIZED);
    }
}
