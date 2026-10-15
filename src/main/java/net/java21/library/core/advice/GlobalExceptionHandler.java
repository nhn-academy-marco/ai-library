package net.java21.library.core.advice;

import lombok.extern.slf4j.Slf4j;
import net.java21.library.core.book.exception.SearchTimeoutException;
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

    @ExceptionHandler(SearchTimeoutException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleSearchTimeout(SearchTimeoutException e, Model model) {
        log.warn("Search timed out: {}", e.getMessage());
        model.addAttribute("message", "검색이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.");
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