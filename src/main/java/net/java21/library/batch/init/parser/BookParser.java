package net.java21.library.batch.init.parser;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public interface BookParser {

    void parse() throws UnsupportedEncodingException;

    default InputStream getInputStream(String file){
        return getClass().getClassLoader().getResourceAsStream(file);
    }
}