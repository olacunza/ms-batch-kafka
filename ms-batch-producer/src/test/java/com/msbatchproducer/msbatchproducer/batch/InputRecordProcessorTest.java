package com.msbatchproducer.msbatchproducer.batch;
import com.msbatchproducer.msbatchproducer.batch.processor.InputRecordProcessor;
import com.msbatchproducer.msbatchproducer.model.dto.XmlInput;
import com.msbatchproducer.msbatchproducer.model.enums.RecordStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class InputRecordProcessorTest {
    private final InputRecordProcessor processor = new InputRecordProcessor("upload");
    @Test void preservesInputAndCreatesPendingWithoutError() throws Exception {
        var record = processor.process(new XmlInput(2, "person.xml", "<root><person firstname='Ana'/></root>"));
        assertThat(record.getBusinessKey()).isEqualTo(2);
        assertThat(record.getUploadId()).isEqualTo("upload");
        assertThat(record.getPayload()).contains("Ana");
        assertThat(record.getStatus()).isEqualTo(RecordStatus.PENDING);
        assertThat(record.getErrorMessage()).isNull();
    }
    @Test void rejectsMalformedXml() throws Exception {
        assertThat(processor.process(new XmlInput(1, "x.xml", "<root>")).getStatus()).isEqualTo(RecordStatus.FAILED);
    }
    @Test void rejectsExternalEntities() throws Exception {
        String xml = "<!DOCTYPE root [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><root>&x;</root>";
        assertThat(processor.process(new XmlInput(1, "x.xml", xml)).getErrorMessage()).contains("DOCTYPE");
    }
}
