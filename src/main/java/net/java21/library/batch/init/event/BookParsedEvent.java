package net.java21.library.batch.init.event;

import net.java21.library.batch.init.dto.BookRawData;

public record BookParsedEvent(BookRawData bookRawData) {}