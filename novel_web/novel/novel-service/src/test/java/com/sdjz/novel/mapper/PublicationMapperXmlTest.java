package com.sdjz.novel.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicationMapperXmlTest {
    @Test
    void mapperStatementsParse() throws Exception {
        String resource = "mapper/PublicationMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String prefix = "com.sdjz.novel.mapper.PublicationMapper.";
        assertTrue(configuration.hasStatement(prefix + "insertSubmission"));
        assertTrue(configuration.hasStatement(prefix + "selectSubmissionPage"));
        assertTrue(configuration.hasStatement(prefix + "copyPublicContents"));
        assertTrue(configuration.hasStatement(prefix + "lockPublicBook"));
        assertTrue(configuration.hasStatement(prefix + "closePendingWithdrawals"));
    }
}
