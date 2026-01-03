package be.appify.prefab.processor.getbyid;

import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import java.util.Optional;

import static org.apache.commons.text.WordUtils.uncapitalize;

class GetByIdServiceWriter {
    MethodSpec getByIdMethod(ClassManifest manifest) {
        TypeName typeName = manifest.type().asTypeName();
        return MethodSpec.methodBuilder("getById")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), typeName))
                .addStatement("log.debug($S, $T.class.getSimpleName(), id)", "Getting {} by id: {}",
                        manifest.className())
                .addStatement(
                        "return %sRepository.findById(id)".formatted(uncapitalize(manifest.simpleName())))
                .build();
    }
}
