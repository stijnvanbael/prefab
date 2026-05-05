package be.appify.prefab.processor;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuilderWriterTest {

    @Test
    void builderWithZeroFieldsContainsOnlyBuildMethod() {
        var code = generateCode("EmptyRecord", List.of());

        assertTrue(code.contains("public static final class Builder"), "Expected nested Builder class");
        assertTrue(code.contains("public static Builder builder()"), "Expected static builder() factory");
        assertTrue(code.contains("public EmptyRecord build()"), "Expected build() method");
        assertFalse(code.contains("withA"), "Expected no setter methods for zero fields");
    }

    @Test
    void builderWithOneFieldHasFluentSetter() {
        var fields = List.of(ParameterSpec.builder(ClassName.get(String.class), "name").build());
        var code = generateCode("NameRecord", fields);

        assertTrue(code.contains("public static final class Builder"), "Expected nested Builder class");
        assertTrue(code.contains("public Builder withName("), "Expected withName setter");
        assertTrue(code.contains("public NameRecord build()"), "Expected build() returning record type");
    }

    @Test
    void builderWithMultipleFieldsHasOneSetterPerField() {
        var fields = List.of(
                ParameterSpec.builder(ClassName.get(String.class), "name").build(),
                ParameterSpec.builder(TypeName.INT, "age").build(),
                ParameterSpec.builder(TypeName.BOOLEAN, "active").build()
        );
        var code = generateCode("PersonRecord", fields);

        assertTrue(code.contains("public Builder withName("), "Expected withName setter");
        assertTrue(code.contains("public Builder withAge("), "Expected withAge setter");
        assertTrue(code.contains("public Builder withActive("), "Expected withActive setter");
    }

    @Test
    void builderSetterStoresFieldAndReturnsBuilder() {
        var fields = List.of(ParameterSpec.builder(ClassName.get(String.class), "value").build());
        var code = generateCode("SimpleRecord", fields);

        assertTrue(code.contains("this.value = value"), "Expected field assignment in setter");
        assertTrue(code.contains("return this"), "Expected return this in setter");
    }

    @Test
    void buildMethodCallsCanonicalConstructor() {
        var fields = List.of(
                ParameterSpec.builder(ClassName.get(String.class), "firstName").build(),
                ParameterSpec.builder(ClassName.get(String.class), "lastName").build()
        );
        var code = generateCode("FullNameRecord", fields);

        assertTrue(code.contains("new FullNameRecord(firstName, lastName)"), "Expected canonical constructor call");
    }

    @Test
    void emptyPrefixProducesPrefixlessSetterNames() {
        var fields = List.of(
                ParameterSpec.builder(ClassName.get(String.class), "name").build(),
                ParameterSpec.builder(TypeName.INT, "age").build()
        );
        var code = generateCodeWithPrefix("PersonRecord", fields, "");

        assertTrue(code.contains("public Builder name("), "Expected prefix-less name() setter");
        assertTrue(code.contains("public Builder age("), "Expected prefix-less age() setter");
        assertFalse(code.contains("withName"), "Expected no withName setter when prefix is empty");
    }

    @Test
    void customPrefixIsUsedForSetterNames() {
        var fields = List.of(ParameterSpec.builder(ClassName.get(String.class), "name").build());
        var code = generateCodeWithPrefix("NameRecord", fields, "set");

        assertTrue(code.contains("public Builder setName("), "Expected setName setter for 'set' prefix");
        assertFalse(code.contains("withName"), "Expected no withName setter when prefix is 'set'");
    }

    private String generateCode(String recordName, List<ParameterSpec> fields) {
        return generateCodeWithPrefix(recordName, fields, "with");
    }

    private String generateCodeWithPrefix(String recordName, List<ParameterSpec> fields, String prefix) {
        var recordType = ClassName.get("com.example", recordName);
        var recordBuilder = TypeSpec.recordBuilder(recordName).addModifiers(Modifier.PUBLIC);
        new BuilderWriter(prefix).enrichWithBuilder(recordBuilder, recordType, fields);
        return JavaFile.builder("com.example", recordBuilder.build()).build().toString();
    }
}
