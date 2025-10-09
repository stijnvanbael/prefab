package be.appify.prefab.processor.spring.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@ReadingConverter
public class ByteArrayToFileConverter implements Converter<byte[], File> {
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
