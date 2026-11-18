package be.appify.prefab.spike.usecase;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/** Generates one thin Spring MVC adapter for each opted-in use-case class. */
@SupportedAnnotationTypes("be.appify.prefab.spike.usecase.UseCase")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class UseCaseProcessor extends AbstractProcessor {
    private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        var methodsByOwner = new HashMap<TypeElement, List<ExecutableElement>>();
        for (var element : roundEnvironment.getElementsAnnotatedWith(UseCase.class)) {
            if (element.getKind() != ElementKind.METHOD) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@UseCase can only annotate a method", element);
                continue;
            }
            var method = (ExecutableElement) element;
            var owner = (TypeElement) method.getEnclosingElement();
            methodsByOwner.computeIfAbsent(owner, ignored -> new ArrayList<>()).add(method);
        }

        methodsByOwner.forEach((owner, methods) -> {
            if (methods.size() != 1) {
                methods.forEach(method -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "A use-case class may expose only one @UseCase method in this prototype", method));
                return;
            }
            var method = methods.getFirst();
            if (isValid(method)) {
                writeEndpoint(owner, method);
            }
        });
        return true;
    }

    private boolean isValid(ExecutableElement method) {
        var annotation = method.getAnnotation(UseCase.class);
        var valid = true;
        if (!method.getModifiers().contains(Modifier.PUBLIC)
                || method.getModifiers().contains(Modifier.STATIC)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@UseCase method must be a public instance method", method);
            valid = false;
        }
        if (method.getParameters().size() != 1 || method.getReturnType().toString().equals("void")) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@UseCase method must take one request and return a response", method);
            valid = false;
        }
        if (!HTTP_METHODS.contains(annotation.method())) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@UseCase method must be one of " + HTTP_METHODS, method);
            valid = false;
        }
        if (!annotation.path().matches("/[A-Za-z0-9_{}./:-]*")) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@UseCase path must start with '/' and contain only route-safe characters", method);
            valid = false;
        }
        return valid;
    }

    private void writeEndpoint(TypeElement owner, ExecutableElement method) {
        var ownerPackage = processingEnv.getElementUtils().getPackageOf(owner).getQualifiedName().toString();
        var endpointPackage = ownerPackage + ".http";
        var endpointName = owner.getSimpleName() + "Endpoint";
        var qualifiedName = endpointPackage + "." + endpointName;
        var requestType = method.getParameters().getFirst().asType();
        var responseType = method.getReturnType();
        var requestMethod = method.getAnnotation(UseCase.class).method();
        var route = method.getAnnotation(UseCase.class).path();
        var source = """
                package %s;

                import %s;
                import jakarta.validation.Valid;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RequestMethod;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public final class %s {
                    private final %s workflow;

                    public %s(%s workflow) {
                        this.workflow = workflow;
                    }

                    @RequestMapping(path = "%s", method = RequestMethod.%s)
                    public %s invoke(
                            @Valid @RequestBody %s request) {
                        return workflow.%s(request);
                    }
                }
                """.formatted(
                endpointPackage,
                owner.getQualifiedName(),
                endpointName,
                owner.getQualifiedName(),
                endpointName,
                owner.getQualifiedName(),
                route,
                requestMethod,
                responseType,
                requestType,
                method.getSimpleName());

        try (Writer writer = processingEnv.getFiler().createSourceFile(qualifiedName, method).openWriter()) {
            writer.write(source);
        } catch (IOException exception) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Could not generate use-case endpoint: " + exception.getMessage(), method);
        }
    }

}
