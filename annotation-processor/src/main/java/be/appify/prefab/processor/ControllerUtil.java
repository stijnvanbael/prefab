package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.rest.Security;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.CodeBlock;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.util.ArrayList;
import java.util.Optional;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static org.apache.commons.text.WordUtils.uncapitalize;
import static org.atteo.evo.inflector.English.plural;

public class ControllerUtil {
    public static String pathOf(ClassManifest manifest) {
        var parentPath = manifest.parent()
                .map(parent -> "%s/{%sId}/".formatted(
                        toKebabCase(plural(parent.type().parameters().getFirst().simpleName())),
                        uncapitalize(parent.name())))
                .orElse("");
        return parentPath + toKebabCase(plural(manifest.simpleName()));
    }

    public static String[] toRequestParams(Sort sort) {
        var params = new ArrayList<String>();
        sort.stream().forEach(order ->
                params.add(order.getProperty() + "," + order.getDirection().name()));
        return params.toArray(new String[0]);
    }

    public static Optional<AnnotationSpec> securedAnnotation(Security security) {
        if (!security.enabled()) {
            return Optional.empty();
        }
        return !security.authority().isEmpty() ?
                Optional.of(AnnotationSpec.builder(PreAuthorize.class)
                        .addMember("value", "$S", "hasAuthority('%s')".formatted(security.authority()))
                        .build()) :
                Optional.of(AnnotationSpec.builder(PreAuthorize.class)
                        .addMember("value", "$S", "isAuthenticated()")
                        .build());
    }

    public static CodeBlock withMockUser(Security security) {
        return security.enabled() ?
                CodeBlock.of("\n.with($T.user(\"test\")$L)",
                        SecurityMockMvcRequestPostProcessors.class,
                        security.authority().isEmpty()
                                ? CodeBlock.of("")
                                : CodeBlock.of(".authorities(new $T($S))",
                                        SimpleGrantedAuthority.class,
                                        security.authority()))
                : CodeBlock.of("");
    }
}
