package com.nhnacademy.library.batch.init.event;

import com.nhnacademy.library.batch.init.dto.BookRawData;

public record BookParsedEvent(BookRawData bookRawData) {}