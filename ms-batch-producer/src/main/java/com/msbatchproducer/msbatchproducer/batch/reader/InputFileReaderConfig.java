package com.msbatchproducer.msbatchproducer.batch.reader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class InputFileReaderConfig {
    @Bean @StepScope
    public XmlArchiveReader inputFileReader(@Value("#{jobParameters['filePath']}") String filePath,
                                           @Value("#{jobParameters['format']}") String format) {
        return new XmlArchiveReader(filePath, "zip".equals(format));
    }
}
