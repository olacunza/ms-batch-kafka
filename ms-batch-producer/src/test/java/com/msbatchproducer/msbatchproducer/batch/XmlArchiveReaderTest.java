package com.msbatchproducer.msbatchproducer.batch;
import com.msbatchproducer.msbatchproducer.batch.reader.XmlArchiveReader;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.ExecutionContext;
import java.nio.file.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;
class XmlArchiveReaderTest {
    @TempDir Path temp;
    Path archive(String... content) throws Exception {
        Path file = temp.resolve("input.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (int i = 0; i < content.length; i++) {
                zip.putNextEntry(new ZipEntry("item" + i + ".xml")); zip.write(content[i].getBytes()); zip.closeEntry();
            }
        }
        return file;
    }
    @Test void readsWholeXmlAndRestartsFromCommittedPosition() throws Exception {
        var path = archive("<root>\n<a/>\n</root>", "<root><b/></root>");
        var reader = new XmlArchiveReader(path.toString(), true);
        var context = new ExecutionContext(); reader.open(context);
        assertThat(reader.read().payload()).contains("<a/>"); reader.update(context); reader.close();
        var restarted = new XmlArchiveReader(path.toString(), true); restarted.open(context);
        assertThat(restarted.read().ordinal()).isEqualTo(2); assertThat(restarted.read()).isNull(); restarted.close();
    }
    @Test void rejectsZipBombSizedEntry() throws Exception {
        var path = archive("x".repeat(1024 * 1024 + 1));
        var reader = new XmlArchiveReader(path.toString(), true); reader.open(new ExecutionContext());
        assertThatThrownBy(reader::read).hasMessageContaining("1 MiB"); reader.close();
    }
    @Test void rejectsEmptyArchive() throws Exception {
        var reader = new XmlArchiveReader(archive().toString(), true); reader.open(new ExecutionContext());
        assertThatThrownBy(reader::read).hasMessageContaining("no contiene"); reader.close();
    }
    @Test void readsSingleXmlOnce() throws Exception {
        var file = temp.resolve("single.xml"); Files.writeString(file, "<root/>");
        var reader = new XmlArchiveReader(file.toString(), false); reader.open(new ExecutionContext());
        assertThat(reader.read().payload()).isEqualTo("<root/>"); assertThat(reader.read()).isNull(); reader.close();
    }
    Path archiveEntries(String... names) throws Exception {
        Path file = temp.resolve("folders.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (var name : names) {
                zip.putNextEntry(new ZipEntry(name));
                if (!name.endsWith("/")) zip.write("<root/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return file;
    }
    @Test void skipsMacMetadataAndDirectoriesAndRestartsByXmlOrdinal() throws Exception {
        var file = archiveEntries("XML-Example/", "__MACOSX/._XML-Example", "XML-Example/one.xml",
            "__MACOSX/XML-Example/._one.xml", "XML-Example/.DS_Store", "XML-Example/._two.xml", "XML-Example/two.xml");
        var context = new ExecutionContext();
        var reader = new XmlArchiveReader(file.toString(), true); reader.open(context);
        assertThat(reader.read().ordinal()).isEqualTo(1); reader.update(context); reader.close();
        var restarted = new XmlArchiveReader(file.toString(), true); restarted.open(context);
        var second = restarted.read();
        assertThat(second.ordinal()).isEqualTo(2);
        assertThat(second.sourceName()).isEqualTo("XML-Example/two.xml");
        assertThat(restarted.read()).isNull(); restarted.close();
    }
    @Test void rejectsArchiveContainingOnlyMetadata() throws Exception {
        var file = archiveEntries("folder/", "__MACOSX/._folder", "folder/.DS_Store");
        var reader = new XmlArchiveReader(file.toString(), true); reader.open(new ExecutionContext());
        assertThatThrownBy(reader::read).hasMessageContaining("no contiene archivos XML"); reader.close();
    }
    @Test void stillRejectsUnsupportedFiles() throws Exception {
        var reader = new XmlArchiveReader(archiveEntries("folder/", "folder/data.csv").toString(), true);
        reader.open(new ExecutionContext());
        assertThatThrownBy(reader::read).hasMessageContaining("data.csv"); reader.close();
    }
    @Test void ignoredMetadataIsAlsoSizeBounded() throws Exception {
        var file = temp.resolve("metadata.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("__MACOSX/._large"));
            zip.write(new byte[1024 * 1024 + 1]); zip.closeEntry();
        }
        var reader = new XmlArchiveReader(file.toString(), true); reader.open(new ExecutionContext());
        assertThatThrownBy(reader::read).hasMessageContaining("1 MiB"); reader.close();
    }
}
