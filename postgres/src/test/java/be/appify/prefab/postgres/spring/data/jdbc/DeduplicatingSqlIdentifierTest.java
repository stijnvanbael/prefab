package be.appify.prefab.postgres.spring.data.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.SqlIdentifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeduplicatingSqlIdentifierTest {

    @Test
    void partiallyOverlappingPrefixIsDeduplicatedNormally() {
        var identifier = new DeduplicatingSqlIdentifier(SqlIdentifier.quoted("first_name"));

        var transformed = identifier.transform(name -> "person_name_" + name);

        assertEquals("person_name_first", transformed.toSql(IdentifierProcessing.NONE));
    }

    @Test
    void fullyMatchingPrefixIsDeduplicatedToBase() {
        var identifier = new DeduplicatingSqlIdentifier(SqlIdentifier.quoted("score"));

        var transformed = identifier.transform(name -> "score_" + name);

        assertEquals("score", transformed.toSql(IdentifierProcessing.NONE));
    }
}
