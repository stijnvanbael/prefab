package be.appify.prefab.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseUtilTest {

    @Test
    void toKebabCaseConvertsSimpleCamelCase() {
        assertEquals("camel-case", CaseUtil.toKebabCase("camelCase"));
    }

    @Test
    void toKebabCaseConvertsMultiWordCamelCase() {
        assertEquals("my-class-name", CaseUtil.toKebabCase("myClassName"));
    }

    @Test
    void toKebabCaseLowercasesAllLetters() {
        assertEquals("order-created-event", CaseUtil.toKebabCase("OrderCreatedEvent"));
    }

    @Test
    void toKebabCaseHandlesSingleWord() {
        assertEquals("order", CaseUtil.toKebabCase("Order"));
    }

    @Test
    void toSnakeCaseConvertsSimpleCamelCase() {
        assertEquals("camel_case", CaseUtil.toSnakeCase("camelCase"));
    }

    @Test
    void toSnakeCaseConvertsMultiWordCamelCase() {
        assertEquals("my_class_name", CaseUtil.toSnakeCase("myClassName"));
    }

    @Test
    void toSnakeCaseLowercasesAllLetters() {
        assertEquals("order_created_event", CaseUtil.toSnakeCase("OrderCreatedEvent"));
    }

    @Test
    void toSnakeCaseHandlesSingleWord() {
        assertEquals("order", CaseUtil.toSnakeCase("Order"));
    }

    @Test
    void toCamelCaseConvertsDotNotation() {
        assertEquals("myClassName", CaseUtil.toCamelCase("my.className"));
    }

    @Test
    void toCamelCaseConvertsUnderscoreNotation() {
        assertEquals("myClassName", CaseUtil.toCamelCase("my_className"));
    }

    @Test
    void toCamelCaseConvertsHyphenNotation() {
        assertEquals("myClassName", CaseUtil.toCamelCase("my-className"));
    }

    @Test
    void toCamelCaseHandlesAlreadyCamelCase() {
        assertEquals("myClassName", CaseUtil.toCamelCase("myClassName"));
    }
}
