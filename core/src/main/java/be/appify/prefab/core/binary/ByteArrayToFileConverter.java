package be.appify.prefab.postgres.spring.data.jdbc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/** Converter to transform a byte array into a temporary File. */
@ReadingConverter
public class ByteArrayToFileConverter implements Converter<byte[], File> {

    /** Constructs a new ByteArrayToFileConverter. */
    public ByteArrayToFileConverter() {
    }

    @Override
    public File convert(byte[] source) {
        try {
            var tempFile = File.createTempFile("prefab-", ".tmp");
            try (var output = new FileOutputStream(tempFile)) {
                new ByteArrayInputStream(source).transferTo(output);
            }
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert Blob to File", e);
        }
    }
}
