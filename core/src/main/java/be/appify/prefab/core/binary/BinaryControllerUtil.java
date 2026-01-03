package be.appify.prefab.core.binary;

import be.appify.prefab.core.domain.Binary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerErrorException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/** Utility class for Binary controller operations */
public class BinaryControllerUtil {

    private BinaryControllerUtil() {
        // Utility class
    }

    /**
     * Convert a Binary object to a ResponseEntity for file download
     * @param binary the Binary object to convert
     * @return ResponseEntity containing the binary data as an InputStreamResource
     */
    public static ResponseEntity<InputStreamResource> toResponseEntity(Binary binary) {
        try {
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + binary.name() + "\"")
                    .contentType(MediaType.parseMediaType(binary.contentType()))
                    .body(new InputStreamResource(new FileInputStream(binary.data())));
        } catch (FileNotFoundException e) {
            throw new ServerErrorException("Failed to resolve binary file", e);
        }
    }
}
