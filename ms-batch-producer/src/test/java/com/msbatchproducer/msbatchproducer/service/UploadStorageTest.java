package com.msbatchproducer.msbatchproducer.service;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
class UploadStorageTest {
    @TempDir Path directory;
    @Test void usesServerGeneratedNameAndStoresBytes() throws Exception {
        var upload = new UploadStorage(directory.toString()).store(new MockMultipartFile("file", "../../person.xml", "application/xml", "<root/>".getBytes()));
        assertThat(Path.of(upload.path()).getParent()).isEqualTo(directory);
        assertThat(Files.readString(Path.of(upload.path()))).isEqualTo("<root/>");
    }
    @Test void rejectsUnsupportedFormat() {
        assertThatThrownBy(() -> new UploadStorage(directory.toString()).store(new MockMultipartFile("file", "x.txt", "text/plain", "1".getBytes())))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
