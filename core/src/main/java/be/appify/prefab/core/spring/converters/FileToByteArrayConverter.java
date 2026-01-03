package be.appify.prefab.core.spring.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/** Converter to transform a File into a byte array. */
@WritingConverter
public class FileToByteArrayConverter implements Converter<File, byte[]> {

    /** Constructs a new FileToByteArrayConverter. */
    public FileToByteArrayConverter() {
    }

    @Override
    public byte[] convert(File source) {
        try (var inputStream = new FileInputStream(source)) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bytes from file: " + source.getAbsolutePath(), e);
        }
    }
}
