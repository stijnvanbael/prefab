package com.example.documents;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class DocumentUploadAcceptanceTest {
    private static final byte[] PDF = "%PDF-1.4 benchmark".getBytes();

    @Autowired
    MockMvc mockMvc;

    @Test
    void uploadedFileCanBeDownloaded() throws Exception {
        var api = new Api(mockMvc);
        var file = new MockMultipartFile("file", "contract.pdf", "application/pdf", PDF);
        var id = Api.idOf(api.multipartWithJson("/documents", "{\"title\": \"Contract\"}", List.of(file)));

        api.get("/documents/{id}", id).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Contract"));

        var download = api.get("/documents/{id}/file", id).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(download.getContentAsByteArray()).isEqualTo(PDF);
        assertThat(download.getContentType()).startsWith("application/pdf");
    }

    @Test
    void otherContentTypesAreRejected() throws Exception {
        var file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        new Api(mockMvc).multipartWithJson("/documents", "{\"title\": \"Notes\"}", List.of(file)).andExpect(status().isBadRequest());
    }

    @Test
    void filesLargerThanOneMegabyteAreRejected() throws Exception {
        var file = new MockMultipartFile("file", "big.png", "image/png", new byte[1024 * 1024 + 1]);
        new Api(mockMvc).multipartWithJson("/documents", "{\"title\": \"Big\"}", List.of(file)).andExpect(status().isBadRequest());
    }

    @Test
    void pngOfExactlyOneMegabyteIsAccepted() throws Exception {
        var file = new MockMultipartFile("file", "ok.png", "image/png", new byte[1024 * 1024]);
        new Api(mockMvc).multipartWithJson("/documents", "{\"title\": \"Ok\"}", List.of(file)).andExpect(status().isCreated());
    }

    @Test
    void missingFilePartIsABadRequest() throws Exception {
        new Api(mockMvc).multipartWithJson("/documents", "{\"title\": \"No file\"}", List.of()).andExpect(status().isBadRequest());
    }
}
