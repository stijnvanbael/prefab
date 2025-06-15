package be.appify.prefab.processor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class BinaryUtil {
    public static File toFile(byte[] bytes) {
        try {
            var tempFile = File.createTempFile("prefab-", ".tmp");
            try (var output = new FileOutputStream(tempFile)) {
                new ByteArrayInputStream(bytes).transferTo(output);
            }
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert Blob to File", e);
        }
    }

    public static byte[] getBytes(File file) {
        try (var inputStream = new FileInputStream(file)) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bytes from file: " + file.getAbsolutePath(), e);
        }
    }
}
