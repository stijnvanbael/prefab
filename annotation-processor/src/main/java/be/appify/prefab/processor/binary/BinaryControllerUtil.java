package be.appify.prefab.processor.binary;

import be.appify.prefab.core.domain.Binary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerErrorException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class BinaryControllerUtil {
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
