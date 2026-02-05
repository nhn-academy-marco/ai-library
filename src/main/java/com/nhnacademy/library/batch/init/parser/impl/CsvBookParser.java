package com.nhnacademy.library.batch.init.parser.impl;

import com.nhnacademy.library.batch.init.event.BookParsedEvent;
import com.nhnacademy.library.batch.init.event.BookParsingCompleteEvent;
import com.nhnacademy.library.batch.init.parser.BooKParser;
import com.nhnacademy.library.batch.init.dto.BookRawData;
import com.nhnacademy.library.batch.init.properties.InitProperties;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@Component
@Slf4j
@RequiredArgsConstructor
public class CsvBookParser implements BooKParser {
    private final InitProperties initProperties;
    private static final String DEFAULT_DATE_FORMAT_NO_LINE = "yyyyMMdd";
    private static final String CSV_HEADER_SEQ = "SEQ_NO";
    private static final String CSV_HEADER_ISBN = "ISBN_THIRTEEN_NO";
    private static final String CSV_HEADER_VOLUME_TITLE = "VLM_NM";
    private static final String CSV_HEADER_TITLE = "TITLE_NM";
    private static final String CSV_HEADER_AUTHOR = "AUTHR_NM";
    private static final String CSV_HEADER_PUBLISHER = "PUBLISHER_NM";
    private static final String CSV_HEADER_PUB_DATE = "PBLICTE_DE";
    private static final String CSV_HEADER_PRICE = "PRC_VALUE";
    private static final String CSV_HEADER_IMAGE = "IMAGE_URL";
    private static final String CSV_HEADER_CONTENT = "BOOK_INTRCN_CN";
    private static final String CSV_HEADER_SUBTITLE = "TITLE_SBST_NM";
    private static final String CSV_HEADER_EDITION_DATE = "TWO_PBLICTE_DE";

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void parse() throws UnsupportedEncodingException {
        log.info("Starting CSV parsing with properties: {}", initProperties);

        try(
                Reader reader = new InputStreamReader(getInputStream(initProperties.getBookFile()), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true)
                        .setTrim(true)
                        .get()
                        .parse(reader)
        ){

            for (CSVRecord record : parser.getRecords()) {
                BookRawData book = new BookRawData();

                // Long 변환
                String seq = record.get(CSV_HEADER_SEQ);
                if (!seq.isBlank()) {
                    book.setId(Long.parseLong(seq));
                }

                book.setIsbn(record.get(CSV_HEADER_ISBN));
                book.setVolumeTitle(record.get(CSV_HEADER_VOLUME_TITLE));
                book.setTitle(record.get(CSV_HEADER_TITLE));
                book.setAuthorName(record.get(CSV_HEADER_AUTHOR));
                book.setPublisherName(record.get(CSV_HEADER_PUBLISHER));

                String pbDate = record.get(CSV_HEADER_PUB_DATE);
                if (!pbDate.isBlank()) {
                    book.setFirstPublishDate(LocalDate.parse(pbDate, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT_NO_LINE)));
                }

                String price = record.get(CSV_HEADER_PRICE);
                if (!price.isBlank()) {
                    book.setPrice(new BigDecimal(price));
                }

                book.setImageUrl(record.get(CSV_HEADER_IMAGE));
                book.setBookContent(record.get(CSV_HEADER_CONTENT));
                book.setSubtitle(record.get(CSV_HEADER_SUBTITLE));

                String editionDate = record.get(CSV_HEADER_EDITION_DATE);
                if (!editionDate.isBlank()) {
                    book.setEditionPublishDate(convertToLocalDate(editionDate));
                }

                applicationEventPublisher.publishEvent(new BookParsedEvent(book));
                log.debug("Parsed book: {}", book);
            }//end for

            applicationEventPublisher.publishEvent(new BookParsingCompleteEvent());

        } catch (IOException e) {
            log.debug("parse error :{}",e.getMessage(),e);
            throw new RuntimeException(e);
        }

    }

    private LocalDate convertToLocalDate(String strDate){

        strDate = strDate.replaceAll("[^0-9]", "");

        if (StringUtils.isEmpty(strDate)) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(CsvBookParser.DEFAULT_DATE_FORMAT_NO_LINE);
            return LocalDate.parse(strDate, formatter);
        } catch (Exception e) {
            return null;
        }
    }
}
