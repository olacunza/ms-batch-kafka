package com.msbatchproducer.msbatchproducer.batch.processor;

import com.msbatchproducer.msbatchproducer.model.dto.XmlInput;
import com.msbatchproducer.msbatchproducer.model.entity.BatchRecord;
import com.msbatchproducer.msbatchproducer.model.enums.RecordStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component @StepScope
public class InputRecordProcessor implements ItemProcessor<XmlInput, BatchRecord> {
    private final String uploadId;
    public InputRecordProcessor(@Value("#{jobParameters['uploadId']}") String uploadId) { this.uploadId = uploadId; }
    @Override public BatchRecord process(XmlInput input) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        BatchRecord record = new BatchRecord();
        record.setBusinessKey(input.ordinal());
        record.setUploadId(uploadId);
        record.setSourceName(input.sourceName());
        record.setPayload(input.payload());
        record.setStatus(RecordStatus.PENDING);
        try {
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
                @Override public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
            });
            var document = builder.parse(new InputSource(new StringReader(input.payload())));
            if (!"root".equals(document.getDocumentElement().getTagName()))
                throw new IllegalArgumentException("El elemento raíz debe ser <root>");
        } catch (org.xml.sax.SAXException | IllegalArgumentException e) {
            record.setStatus(RecordStatus.FAILED);
            String message = "XML inválido: " + e.getMessage();
            record.setErrorMessage(message.substring(0, Math.min(2000, message.length())));
        }
        var now = LocalDateTime.now(ZoneOffset.UTC);
        record.setCreatedAt(now); record.setUpdatedAt(now);
        return record;
    }
}
