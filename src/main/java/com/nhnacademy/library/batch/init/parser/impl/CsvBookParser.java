package com.nhnacademy.library.batch.init.parser.impl;

import com.nhnacademy.library.batch.init.event.BookParsedEvent;
import com.nhnacademy.library.batch.init.event.BookParsingComplateEvent;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class CsvBookParser implements BooKParser {
    private final InitProperties initProperties;
    private static final String DEFAULT_DATE_FORMAT_NO_LINE = "yyyyMMdd";
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void parse() throws UnsupportedEncodingException {
        log.info("initProperties:{}",initProperties);

        List<BookRawData> books = new ArrayList<>();
        try(
                Reader reader = new InputStreamReader(getInputStream(initProperties.getBookFile()), "UTF-8");
                CSVParser parser = CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .withIgnoreHeaderCase()
                        .withTrim()
                        .parse(reader);
        ){

            for (CSVRecord record : parser.getRecords()) {
                BookRawData book = new BookRawData();

                // Long 변환
                String seq = record.get("SEQ_NO");
                if (!seq.isBlank()) {
                    book.setId(Long.parseLong(seq));
                }

                book.setIsbn(record.get("ISBN_THIRTEEN_NO"));
                book.setVolumeTitle(record.get("VLM_NM"));
                book.setTitle(record.get("TITLE_NM"));
                book.setAuthorName(record.get("AUTHR_NM"));
                book.setPublisherName(record.get("PUBLISHER_NM"));

                String pbDate = record.get("PBLICTE_DE");
                if (!pbDate.isBlank()) {
                    book.setFirstPublishDate(LocalDate.parse(pbDate, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT_NO_LINE)));
                }

                String price = record.get("PRC_VALUE");
                if (!price.isBlank()) {
                    book.setPrice(new BigDecimal(price));
                }

                book.setImageUrl(record.get("IMAGE_URL"));
                book.setBookContent(record.get("BOOK_INTRCN_CN"));
                book.setSubtitle(record.get("TITLE_SBST_NM"));

                String editionDate = record.get("TWO_PBLICTE_DE");
                if (!editionDate.isBlank()) {
                    book.setEditionPublishDate(convertToLocalDate(editionDate));
                }

                applicationEventPublisher.publishEvent(new BookParsedEvent(book));

                log.info("book:{}",book);
                //books.add(book);
            }//end for

            applicationEventPublisher.publishEvent(new BookParsingComplateEvent());

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

    private String removeQuote(String str) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        if ((str.startsWith("\"") && str.endsWith("\"")) || str.equals("\"\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str.trim();
    }
}
