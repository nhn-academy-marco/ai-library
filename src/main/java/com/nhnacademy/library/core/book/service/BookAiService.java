package com.nhnacademy.library.core.book.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookAiService {

    private final ChatModel chatModel;

    public String askAboutBooks(String promptText) {
        return chatModel.call(promptText);
    }
}
