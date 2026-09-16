package com.msbatchproducer.msbatchproducer.batch.reader;

import com.msbatchproducer.msbatchproducer.model.dto.XmlInput;
import org.springframework.batch.item.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.zip.*;

/** Reads one bounded XML at a time. ZIP entries are never extracted to disk. */
public class XmlArchiveReader extends ItemStreamSupport implements ItemStreamReader<XmlInput> {
    private static final int MAX_XML_BYTES = 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 100L * 1024 * 1024;
    private final Path path;
    private final boolean zip;
    private ZipInputStream archive;
    private long index;
    private long totalBytes;
    private int entryCount;
    public XmlArchiveReader(String path, boolean zip) { this.path = Path.of(path); this.zip = zip; }
    @Override public void open(ExecutionContext context) {
        try {
            index = 0; totalBytes = 0; entryCount = 0;
            if (zip) archive = new ZipInputStream(Files.newInputStream(path), StandardCharsets.UTF_8);
            long saved = context.getLong("xml.index", 0L);
            for (long i = 0; i < saved; i++) {
                if (read() == null) throw new IOException("El archivo cambió desde la ejecución anterior");
            }
        } catch (Exception e) { throw new ItemStreamException("No se pudo abrir la carga XML", e); }
    }
    @Override public XmlInput read() throws IOException {
        if (!zip) {
            if (index > 0) return null;
            try (InputStream in = Files.newInputStream(path)) {
                return new XmlInput(++index, "document.xml", readBounded(in));
            }
        }
        ZipEntry entry;
        while (true) {
            entry = archive.getNextEntry();
            if (entry == null) {
                if (index == 0) throw new IOException("El ZIP no contiene archivos XML");
                return null;
            }
            if (++entryCount > 30000) throw new IOException("Máximo 30000 entradas ZIP por carga");
            String name = entry.getName();
            String basename = name.substring(name.lastIndexOf('/') + 1);
            // ZIPs produced by Finder contain directories and AppleDouble metadata.
            // Consume ignored entries with the same byte limits; never extract paths.
            if (entry.isDirectory() || name.startsWith("__MACOSX/")
                    || basename.equals(".DS_Store") || basename.startsWith("._")) {
                readBounded(archive);
                archive.closeEntry();
                continue;
            }
            if (!name.toLowerCase(Locale.ROOT).endsWith(".xml")) {
                throw new IOException("El ZIP debe contener solo archivos XML: " + name);
            }
            break;
        }
        if (++index > 10000) throw new IOException("Máximo 10000 XML por carga");
        if (entry.getName().length() > 512) throw new IOException("Nombre de XML demasiado largo");
        String xml = readBounded(archive);
        archive.closeEntry();
        return new XmlInput(index, entry.getName(), xml);
    }
    private String readBounded(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(MAX_XML_BYTES + 1);
        if (bytes.length > MAX_XML_BYTES) throw new IOException("Cada XML debe ocupar como máximo 1 MiB");
        totalBytes += bytes.length;
        if (totalBytes > MAX_TOTAL_BYTES) throw new IOException("El ZIP supera 100 MiB descomprimidos");
        return new String(bytes, StandardCharsets.UTF_8);
    }
    @Override public void update(ExecutionContext context) { context.putLong("xml.index", index); }
    @Override public void close() {
        if (archive != null) try { archive.close(); } catch (IOException e) { throw new ItemStreamException(e); }
    }
}
