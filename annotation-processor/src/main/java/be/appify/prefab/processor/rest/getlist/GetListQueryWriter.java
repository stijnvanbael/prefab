package be.appify.prefab.processor.rest.getlist;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.FileOutput;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import org.springframework.data.domain.Sort;

import static be.appify.prefab.processor.rest.getlist.GetListUtil.filterPropertiesOf;
import static org.apache.commons.text.WordUtils.uncapitalize;

class GetListQueryWriter {
    static final int DEFAULT_PAGE_SIZE = 20;

    static String queryName(ClassManifest manifest) {
        return "%sQuery".formatted(manifest.simpleName());
    }

    static ClassName queryType(ClassManifest manifest) {
        return ClassName.get(manifest.packageName() + ".application", queryName(manifest));
    }

    static String parentIdName(ClassManifest manifest) {
        return manifest.parent().map(parent -> uncapitalize(parent.name()) + "Id").orElseThrow();
    }

    void writeQuery(FileOutput fileWriter, ClassManifest manifest) {
        var queryType = queryType(manifest);
        var type = TypeSpec.classBuilder(queryType)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Fluent query for {@code $L.getList}. Filters that are not set are ignored.\n",
                        manifest.simpleName() + "Service")
                .addField(FieldSpec.builder(int.class, "page", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(int.class, "pageSize", Modifier.PRIVATE)
                        .initializer("$L", DEFAULT_PAGE_SIZE).build())
                .addField(FieldSpec.builder(Sort.class, "sort", Modifier.PRIVATE).build())
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                .addMethod(MethodSpec.methodBuilder("newQuery")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(queryType)
                        .addStatement("return new $T()", queryType)
                        .build())
                .addMethod(setter(queryType, "page", TypeName.INT))
                .addMethod(getter("page", TypeName.INT, "page"))
                .addMethod(setter(queryType, "pageSize", TypeName.INT))
                .addMethod(getter("pageSize", TypeName.INT, "pageSize"))
                .addMethod(setter(queryType, "sort", ClassName.get(Sort.class)))
                .addMethod(optionalGetter("sort", ClassName.get(Sort.class)));
        manifest.parent().ifPresent(parent -> {
            var parentIdName = parentIdName(manifest);
            type.addField(FieldSpec.builder(String.class, parentIdName, Modifier.PRIVATE).build())
                    .addMethod(setter(queryType, parentIdName, ClassName.get(String.class)))
                    .addMethod(optionalGetter(parentIdName, ClassName.get(String.class)));
        });
        filterPropertiesOf(manifest).forEach(filter -> {
            var name = filter.parameterName();
            var filterType = GetListServiceWriter.filterParamType(filter);
            type.addField(FieldSpec.builder(filterType, name, Modifier.PRIVATE).build())
                    .addMethod(setter(queryType, name, filterType))
                    .addMethod(optionalGetter(name, filterType));
        });
        fileWriter.writeFile(manifest.packageName(), queryName(manifest), type.build());
    }

    private static MethodSpec setter(ClassName queryType, String name, TypeName valueType) {
        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .returns(queryType)
                .addParameter(valueType, name)
                .addStatement("this.$N = $N", name, name)
                .addStatement("return this")
                .build();
    }

    private static MethodSpec getter(String name, TypeName type, String field) {
        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .returns(type)
                .addStatement("return $N", field)
                .build();
    }

    private static MethodSpec optionalGetter(String name, TypeName valueType) {
        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), valueType))
                .addStatement("return $T.ofNullable($N)", Optional.class, name)
                .build();
    }
}
