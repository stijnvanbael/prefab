package be.appify.prefab.core.spring;

import be.appify.prefab.core.domain.Binary;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/** Service for storing files temporarily. */
@Component
public class StorageService {

    StorageService() {
    }

    /**
     * Stores the given MultipartFile as a temporary Binary file.
     *
     * @param file the MultipartFile to store
     * @return a Binary representing the stored file
     * @throws RuntimeException if an I/O error occurs during file storage
     */
    public Binary store(MultipartFile file) {
        try {
            var tempFile = File.createTempFile("prefab-", ".tmp");
            file.transferTo(tempFile);
            tempFile.deleteOnExit();
            return new Binary(file.getOriginalFilename(), file.getContentType(), tempFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not store file: " + file.getOriginalFilename(), e);
        }
    }
}
