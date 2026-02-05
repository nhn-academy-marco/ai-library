package com.nhnacademy.library.front.web;

import com.nhnacademy.library.core.review.dto.ReviewCreateRequest;
import com.nhnacademy.library.core.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/books/{bookId}/reviews")
@RequiredArgsConstructor
public class BookReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public String createReview(@PathVariable("bookId") Long bookId,
                               @Valid @ModelAttribute ReviewCreateRequest reviewCreateRequest,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        
        log.debug("POST /books/{}/reviews with request: {}", bookId, reviewCreateRequest);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 내용을 확인해주세요 (1~5점, 내용은 필수입니다).");
            return "redirect:/books/" + bookId;
        }

        try {
            reviewService.createReview(bookId, reviewCreateRequest);
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 등록되었습니다.");
        } catch (Exception e) {
            log.error("Failed to create review", e);
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 등록 중 오류가 발생했습니다.");
        }

        return "redirect:/books/" + bookId;
    }
}
