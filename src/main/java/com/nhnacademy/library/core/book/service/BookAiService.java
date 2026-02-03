package com.nhnacademy.library.core.book.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class BookAiService {

    private final ChatModel chatModel;

    public BookAiService(@Qualifier("googleGenAiChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String askAboutBooks(String promptText) {
        return chatModel.call(promptText);
    }
}
