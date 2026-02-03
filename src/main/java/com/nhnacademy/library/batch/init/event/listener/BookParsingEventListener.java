package com.nhnacademy.library.batch.init.event.listener;

import com.nhnacademy.library.batch.init.event.BookParsedEvent;
import com.nhnacademy.library.batch.init.event.BookParsingComplateEvent;
import com.nhnacademy.library.batch.init.parser.BooKParser;
import com.nhnacademy.library.batch.init.dto.BookRawData;
import com.nhnacademy.library.batch.init.properties.InitProperties;
import com.nhnacademy.library.batch.init.service.BookBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookParsingEventListener {

    private final InitProperties initProperties;
    private final BooKParser bookParser;
    private final List<BookRawData> buffer = new ArrayList<>();
    private final BookBatchService bookbatchService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if(initProperties.isEnable()) {
            try {
                bookParser.parse();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @EventListener
    public void onBookParsed(BookParsedEvent event){
        buffer.add(event.bookRawData());
    }

    @EventListener
    public void onParsingCompleted(BookParsingComplateEvent event){
        bookbatchService.initializeBooks(buffer, initProperties.getBatchSize());
    }

}
