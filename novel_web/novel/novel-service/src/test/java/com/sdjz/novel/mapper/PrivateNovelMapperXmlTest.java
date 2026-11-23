package com.sdjz.novel.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateNovelMapperXmlTest {
    @Test
    void mapperStatementsParse() throws Exception {
        String resource = "mapper/PrivateNovelMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            XMLMapperBuilder builder = new XMLMapperBuilder(input, configuration, resource,
                    configuration.getSqlFragments());
            builder.parse();
        }
        assertTrue(configuration.hasStatement("com.sdjz.novel.mapper.PrivateNovelMapper.selectNovel"));
        assertTrue(configuration.hasStatement("com.sdjz.novel.mapper.PrivateNovelMapper.selectChapter"));
        assertTrue(configuration.hasStatement("com.sdjz.novel.mapper.PrivateNovelMapper.upsertProgress"));
    }
}
