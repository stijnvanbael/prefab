package be.appify.prefab.processor.rest.delete;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import com.palantir.javapoet.MethodSpec;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.uncapitalize;

class DeleteServiceWriter {
    MethodSpec deleteMethod(ClassManifest manifest) {
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        var tenantField = manifest.tenantIdField();
        var method = MethodSpec.methodBuilder("delete")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .addStatement("log.debug($S, $T.class.getSimpleName(), id)", "Deleting {} with id: {}",
                        manifest.className());
        if (tenantField.isPresent()) {
            var tf = tenantField.get();
            method.addStatement("""
                            $N.findById(id)
                                    .filter(aggregate -> tenantContextProvider.currentTenantId() == null
                                            || aggregate.$N().equals(tenantContextProvider.currentTenantId()))
                                    .ifPresent(aggregate -> $N.deleteById(id))""",
                    repositoryName, tf.name(), repositoryName);
        } else {
            method.addStatement("%sRepository.deleteById(id)".formatted(uncapitalize(manifest.simpleName())));
        }
        return method.build();
    }

    public MethodSpec deleteMethod(ClassManifest manifest, ExecutableElement method) {
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        var tenantField = manifest.tenantIdField();
        var builder = MethodSpec.methodBuilder("delete")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .addStatement("log.debug($S, $T.class.getSimpleName(), id)", "Deleting {} with id: {}",
                        manifest.className());
        if (tenantField.isPresent()) {
            var tf = tenantField.get();
            builder.addStatement("""
                            $N.findById(id)
                                    .filter(aggregate -> tenantContextProvider.currentTenantId() == null
                                            || aggregate.$N().equals(tenantContextProvider.currentTenantId()))
                                    .ifPresent(aggregate -> {
                                        aggregate.$N();
                                        $N.deleteById(id);
                                    })""",
                    repositoryName, tf.name(), method.getSimpleName(), repositoryName);
        } else {
            builder.addStatement("%sRepository.findById(id).ifPresent(aggregate -> aggregate.%s())"
                    .formatted(uncapitalize(manifest.simpleName()), method.getSimpleName()));
            builder.addStatement("%sRepository.deleteById(id)".formatted(uncapitalize(manifest.simpleName())));
        }
        return builder.build();
    }

    MethodSpec deleteMethodForPolymorphic(PolymorphicAggregateManifest manifest) {
        return MethodSpec.methodBuilder("delete")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .addStatement("log.debug($S, $T.class.getSimpleName(), id)", "Deleting {} with id: {}",
                        manifest.className())
                .addStatement("%sRepository.deleteById(id)".formatted(uncapitalize(manifest.simpleName())))
                .build();
    }
}

