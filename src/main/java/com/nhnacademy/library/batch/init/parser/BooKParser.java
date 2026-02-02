package com.nhnacademy.library.batch.init.parser;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public interface BooKParser {

    void parse() throws UnsupportedEncodingException;

    default InputStream getInputStream(String file){
        return getClass().getClassLoader().getResourceAsStream(file);
    }
}