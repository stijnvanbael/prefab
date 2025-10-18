package be.appify.prefab.processor.search;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.text.WordUtils.uncapitalize;

import javax.lang.model.element.Modifier;

public class SearchServiceWriter {
    public MethodSpec searchMethod(ClassManifest manifest, VariableManifest searchProperty) {
        var method = MethodSpec.methodBuilder("search")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Pageable.class, "pageable");
        manifest.parent().ifPresent(parent -> method.addParameter(String.class,
                uncapitalize(parent.name()) + "Id"));
        if (searchProperty != null) {
            method.addParameter(searchProperty.type().asTypeName(), searchProperty.name());
        }
        TypeName typeName = manifest.type().asTypeName();
        method.returns(ParameterizedTypeName.get(ClassName.get(Page.class), typeName));
        if (searchProperty != null) {
            method.addStatement("log.debug($S, $T.class.getSimpleName(), $S, $L)",
                    "Searching {} by {}: {}", manifest.className(), searchProperty.name(), searchProperty.name());
        } else {
            method.addStatement("log.debug($S, $T.class.getSimpleName())", "Searching all {}", manifest.className());
        }
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        if (searchProperty == null) {
            method.addStatement("return $N.$N($L)",
                    repositoryName,
                    "find%s".formatted(
                            manifest.parent().map(parent -> "By" + capitalize(parent.name())).orElse("All")),
                    manifest.parent().map(parent -> parent.name() + "Id, ").orElse("") + "pageable");
        } else {
            method.addStatement("""
                            return !$T.isBlank($N)
                                ? $N.$N($L, pageable)
                                : $N.$N($L)""",
                    StringUtils.class,
                    searchProperty.name(),
                    repositoryName,
                    "findBy%sLike".formatted(
                            manifest.parent().map(parent -> capitalize(parent.name()) + "And").orElse("")
                            + capitalize(searchProperty.name())),
                    manifest.parent().map(parent -> parent.name() + "Id, ").orElse("") + searchProperty.name(),
                    repositoryName,
                    "find%s".formatted(
                            manifest.parent().map(parent -> "By" + capitalize(parent.name())).orElse("All")),
                    manifest.parent().map(parent -> parent.name() + "Id, ").orElse("") + "pageable"
            );
        }
        return method.build();
    }
}
