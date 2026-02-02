package com.nhnacademy.library.core.book.dto;

public record BookSearchRequest(
        String keyword,
        String isbn
) {}