package com.nhnacademy.library.core.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 전역 예외 핸들러
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception e, Model model) {
        log.error("Unhandled exception occurred", e);
        model.addAttribute("message", "서버 내부 오류가 발생했습니다.");
        return "common/error/error";
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleRuntimeException(RuntimeException e, Model model) {
        log.warn("Runtime exception occurred: {}", e.getMessage());
        model.addAttribute("message", e.getMessage());
        return "common/error/error";
    }
}