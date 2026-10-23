package be.appify.prefab.mongodb.spring.data.mongodb;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoTextIndexesTest {

    private final IndexOperations operations = mock(IndexOperations.class);
    private final TextIndexDefinition definition = TextIndexDefinition.builder().onFields("title").build();

    private static IndexInfo textIndex(String name, String language) {
        return new IndexInfo(List.of(IndexField.text("title", 1f)), name, false, false, language);
    }

    @Test
    void createOrRebuild_givenTextIndexWithOtherLanguage_dropsItBeforeCreating() {
        when(operations.getIndexInfo()).thenReturn(List.of(textIndex("title_text", "none")));

        MongoTextIndexes.createOrRebuild(operations, definition, "english");

        var order = inOrder(operations);
        order.verify(operations).dropIndex("title_text");
        order.verify(operations).createIndex(definition);
    }

    @Test
    void createOrRebuild_givenTextIndexWithSameLanguage_keepsIt() {
        when(operations.getIndexInfo()).thenReturn(List.of(textIndex("title_text", "english")));

        MongoTextIndexes.createOrRebuild(operations, definition, "english");

        verify(operations, never()).dropIndex(anyString());
        verify(operations).createIndex(definition);
    }
}
