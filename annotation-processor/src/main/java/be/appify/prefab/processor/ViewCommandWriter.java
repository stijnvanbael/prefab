package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Exposure;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.view.ViewCommands;
import be.appify.prefab.processor.rest.ControllerUtil;
import be.appify.prefab.processor.rest.PathVariables;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

/** Generates HTTP adapters for annotated methods on {@link ViewCommands} components. */
final class ViewCommandWriter {
    private static final String COMMANDS_TYPE = ViewCommands.class.getCanonicalName();

    private final Set<String> writtenCommandTypes = new HashSet<>();

    void writeCommands(PrefabContext context, RoundEnvironment roundEnvironment) {
        roundEnvironment.getRootElements().stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .filter(type -> !type.getKind().isInterface() && !type.getModifiers().contains(Modifier.ABSTRACT))
                .filter(type -> writtenCommandTypes.add(type.getQualifiedName().toString()))
                .forEach(type -> commandAggregate(context, type)
                        .ifPresent(aggregate -> writeCommandsFor(context, type, aggregate)));
    }

    boolean writeCreateForm(
            PrefabContext context,
            TypeElement formType,
            TypeElement aggregateType,
            Create create
    ) {
        var processingEnvironment = context.processingEnvironment();
        var aggregate = ClassManifest.of(aggregateType, processingEnvironment);
        var form = ClassManifest.of(formType, processingEnvironment);
        var requestTypeName = aggregate.packageName() + ".application.Create" + aggregate.simpleName() + "Request";
        var hasToRequest = formType.getEnclosedElements().stream()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .anyMatch(method -> method.getKind() == ElementKind.METHOD
                        && method.getSimpleName().contentEquals("toRequest")
                        && method.getModifiers().contains(Modifier.PUBLIC)
                        && method.getParameters().isEmpty()
                        && method.getReturnType().getKind() != javax.lang.model.type.TypeKind.VOID);
        var createConstructors = aggregateType.getEnclosedElements().stream()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .filter(constructor -> constructor.getKind() == ElementKind.CONSTRUCTOR)
                .filter(constructor -> constructor.getAnnotation(Create.class) != null)
                .filter(constructor -> !constructor.getParameters().isEmpty())
                .toList();
        var isSupported = form.type().isRecord()
                && aggregate.parent().isEmpty()
                && create.path().isBlank()
                && createConstructors.size() == 1
                && hasToRequest;
        if (!isSupported) {
            context.logError("[@ViewOf @Create] " + form.simpleName()
                    + ": create forms require a record with a public toRequest() method returning "
                    + requestTypeName + ", an aggregate with one request-based @Create constructor, "
                    + "and no aggregate parent or custom command path.", formType);
            return false;
        }

        var serviceType = ClassName.get(form.packageName() + ".application", form.simpleName() + "Service");
        var aggregateServiceType = ClassName.get(aggregate.packageName() + ".application",
                aggregate.simpleName() + "Service");
        var service = TypeSpec.classBuilder(serviceType)
                .addModifiers(PUBLIC)
                .addAnnotation(Component.class)
                .addAnnotation(Transactional.class)
                .addField(aggregateServiceType, "aggregateService", PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(PUBLIC)
                        .addParameter(aggregateServiceType, "aggregateService")
                        .addStatement("this.aggregateService = aggregateService")
                        .build())
                .addMethod(MethodSpec.methodBuilder("create")
                        .addModifiers(PUBLIC)
                        .returns(String.class)
                        .addParameter(TypeName.get(formType.asType()), "form")
                        .addStatement("return aggregateService.create(form.toRequest())")
                        .build());
        new JavaFileWriter(context.processingEnvironment(), "application")
                .writeFile(form.packageName(), serviceType.simpleName(), service.build());
        writeCreateFormController(context, formType, aggregate, form, create, serviceType);
        if (isPublic(create.exposure())) {
            new TestClientWriter(context).writeViewCreateFormTestSupport(formType, form, create);
        }
        return true;
    }

    private static void writeCreateFormController(
            PrefabContext context,
            TypeElement formType,
            ClassManifest aggregate,
            ClassManifest form,
            Create create,
            ClassName serviceType
    ) {
        var controller = TypeSpec.classBuilder(form.simpleName() + "Controller")
                .addModifiers(PUBLIC)
                .addAnnotation(RestController.class)
                .addAnnotation(AnnotationSpec.builder(RequestMapping.class)
                        .addMember("path", "$S", basePath(formType))
                        .build())
                .addField(serviceType, "service", PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(PUBLIC)
                        .addParameter(serviceType, "service")
                        .addStatement("this.service = service")
                        .build());
        if (isPublic(create.exposure())) {
            var method = MethodSpec.methodBuilder("create")
                    .addModifiers(PUBLIC)
                    .addAnnotation(requestMapping(create.method(), create.path()))
                    .returns(ParameterizedTypeName.get(ResponseEntity.class, Void.class))
                    .addParameter(ParameterSpec.builder(TypeName.get(formType.asType()), "form")
                            .addAnnotation(RequestBody.class)
                            .addAnnotation(Valid.class)
                            .build())
                    .addStatement("var id = service.create(form)")
                    .addStatement("return $T.created($T.create($S + id)).build()",
                            ResponseEntity.class, URI.class, "/" + toKebabCase(aggregate.plural()) + "/");
            ControllerUtil.operationAnnotation("Create " + form.simpleName()).ifPresent(method::addAnnotation);
            ControllerUtil.securedAnnotation(create.security()).ifPresent(method::addAnnotation);
            controller.addMethod(method.build());
        }
        new JavaFileWriter(context.processingEnvironment(), "infrastructure.http")
                .writeFile(form.packageName(), form.simpleName() + "Controller", controller.build());
    }

    private static boolean isPublic(Exposure exposure) {
        return exposure != Exposure.INTERNAL;
    }

    private java.util.Optional<TypeElement> commandAggregate(PrefabContext context, TypeElement type) {
        return commandAggregate(context, type.asType(), new HashSet<>());
    }

    private java.util.Optional<TypeElement> commandAggregate(
            PrefabContext context,
            TypeMirror type,
            Set<String> visited
    ) {
        if (!(type instanceof DeclaredType declared) || !visited.add(type.toString())) {
            return java.util.Optional.empty();
        }
        var element = (TypeElement) declared.asElement();
        if (element.getQualifiedName().contentEquals(COMMANDS_TYPE)) {
            if (declared.getTypeArguments().size() != 1) {
                return java.util.Optional.empty();
            }
            var aggregate = context.processingEnvironment().getTypeUtils()
                    .asElement(declared.getTypeArguments().getFirst());
            return aggregate instanceof TypeElement aggregateType
                    ? java.util.Optional.of(aggregateType)
                    : java.util.Optional.empty();
        }
        return context.processingEnvironment().getTypeUtils().directSupertypes(declared).stream()
                .map(parent -> commandAggregate(context, parent, visited))
                .flatMap(java.util.Optional::stream)
                .findFirst();
    }

    private void writeCommandsFor(PrefabContext context, TypeElement commandType, TypeElement aggregateType) {
        if (aggregateType.getAnnotation(be.appify.prefab.core.annotations.Aggregate.class) == null) {
            context.logError("[ViewCommands] " + commandType.getSimpleName()
                    + ": generic type must be an @Aggregate.\n"
                    + "Suggested fix: implement ViewCommands<YourAggregate>.", commandType);
            return;
        }
        var operations = commandType.getEnclosedElements().stream()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .filter(method -> method.getKind() == ElementKind.METHOD)
                .filter(method -> method.getModifiers().contains(Modifier.PUBLIC))
                .map(method -> commandOperation(method, context))
                .flatMap(java.util.Optional::stream)
                .toList();
        if (operations.isEmpty()) {
            return;
        }
        validateUniqueMappings(context, commandType, operations);
        var model = new CommandModel(commandType, aggregateType, operations);
        writeService(context, model);
        writeController(context, model);
        new TestClientWriter(context).writeViewCommandTestSupport(model);
    }

    private java.util.Optional<CommandOperation> commandOperation(
            ExecutableElement method,
            PrefabContext context
    ) {
        var create = method.getAnnotation(Create.class);
        var update = method.getAnnotation(Update.class);
        var delete = method.getAnnotation(Delete.class);
        var count = (create == null ? 0 : 1) + (update == null ? 0 : 1) + (delete == null ? 0 : 1);
        if (count == 0) {
            return java.util.Optional.empty();
        }
        if (count > 1 || method.getModifiers().contains(Modifier.STATIC)) {
            context.logError("[ViewCommands] " + method.getSimpleName()
                    + ": declare exactly one of @Create, @Update, or @Delete on a public instance method.",
                    method);
            return java.util.Optional.empty();
        }
        var path = create != null ? create.path()
                : update != null ? "/{id}" + update.path()
                : delete.path();
        var methodName = create != null ? create.method()
                : update != null ? update.method()
                : delete.method();
        var security = create != null ? create.security()
                : update != null ? update.security()
                : delete.security();
        var exposure = create != null ? create.exposure()
                : update != null ? update.exposure()
                : delete.exposure();
        var pathVariables = new HashSet<>(PathVariables.extractFrom(path));
        if (update != null || delete != null) {
            pathVariables.add("id");
        }
        var bodyParameters = method.getParameters().stream()
                .filter(parameter -> !pathVariables.contains(parameter.getSimpleName().toString()))
                .toList();
        if (bodyParameters.size() > 1) {
            context.logError("[ViewCommands] " + method.getSimpleName()
                    + ": at most one body parameter is supported.\n"
                    + "Suggested fix: wrap the values in a request record.", method);
            return java.util.Optional.empty();
        }
        var unmappedPathVariables = pathVariables.stream()
                .filter(pathName -> method.getParameters().stream()
                        .noneMatch(parameter -> parameter.getSimpleName().contentEquals(pathName)))
                .toList();
        if (!unmappedPathVariables.isEmpty()) {
            context.logError("[ViewCommands] " + method.getSimpleName() + ": path variable(s) "
                    + unmappedPathVariables + " must be method parameters.", method);
            return java.util.Optional.empty();
        }
        var mapping = "%s|%s".formatted(methodName, path);
        return java.util.Optional.of(new CommandOperation(method, methodName, path, security, exposure,
                Set.copyOf(pathVariables), bodyParameters.stream().map(p -> p.getSimpleName().toString())
                        .collect(java.util.stream.Collectors.toSet()), mapping));
    }

    private void validateUniqueMappings(
            PrefabContext context,
            TypeElement commandType,
            List<CommandOperation> operations
    ) {
        operations.stream()
                .collect(java.util.stream.Collectors.groupingBy(CommandOperation::mapping))
                .values().stream()
                .filter(group -> group.size() > 1)
                .flatMap(group -> group.stream().skip(1))
                .forEach(operation -> context.logError("[ViewCommands] " + operation.method().getSimpleName()
                        + ": multiple command methods share HTTP method and path " + operation.mapping() + ".",
                        operation.method()));
    }

    private void writeService(PrefabContext context, CommandModel model) {
        var commandType = ClassName.get(model.commandType());
        var serviceType = ClassName.get(packageName(model.commandType()) + ".application",
                model.commandType().getSimpleName() + "Service");
        var commandsField = FieldSpec.builder(commandType, "commands", PRIVATE, Modifier.FINAL).build();
        var service = TypeSpec.classBuilder(serviceType)
                .addModifiers(PUBLIC)
                .addAnnotation(Component.class)
                .addAnnotation(Transactional.class)
                .addField(commandsField)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(PUBLIC)
                        .addParameter(commandType, "commands")
                        .addStatement("this.commands = commands")
                        .build());
        model.operations().forEach(operation -> service.addMethod(serviceMethod(operation)));
        new JavaFileWriter(context.processingEnvironment(), "application")
                .writeFile(packageName(model.commandType()), serviceType.simpleName(), service.build());
    }

    private static MethodSpec serviceMethod(CommandOperation operation) {
        var sourceMethod = operation.method();
        var method = MethodSpec.methodBuilder(sourceMethod.getSimpleName().toString())
                .addModifiers(PUBLIC)
                .returns(TypeName.get(sourceMethod.getReturnType()))
                .addParameters(sourceMethod.getParameters().stream()
                        .map(parameter -> ParameterSpec.builder(
                                TypeName.get(parameter.asType()), parameter.getSimpleName().toString()).build())
                        .toList())
                .addExceptions(sourceMethod.getThrownTypes().stream().map(TypeName::get).toList());
        var args = sourceMethod.getParameters().stream()
                .map(parameter -> parameter.getSimpleName().toString())
                .collect(java.util.stream.Collectors.joining(", "));
        if (sourceMethod.getReturnType().toString().equals("void")) {
            method.addStatement("commands.$N($L)", sourceMethod.getSimpleName(), args);
        } else {
            method.addStatement("return commands.$N($L)", sourceMethod.getSimpleName(), args);
        }
        return method.build();
    }

    private void writeController(PrefabContext context, CommandModel model) {
        var serviceType = ClassName.get(packageName(model.commandType()) + ".application",
                model.commandType().getSimpleName() + "Service");
        var controllerType = ClassName.get(packageName(model.commandType()),
                model.commandType().getSimpleName() + "Controller");
        var controller = TypeSpec.classBuilder(controllerType)
                .addModifiers(PUBLIC)
                .addAnnotation(RestController.class)
                .addAnnotation(Validated.class)
                .addAnnotation(AnnotationSpec.builder(RequestMapping.class)
                        .addMember("path", "$S", basePath(model.commandType()))
                        .build())
                .addField(serviceType, "service", PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(PUBLIC)
                        .addParameter(serviceType, "service")
                        .addStatement("this.service = service")
                        .build());
        ControllerUtil.tagAnnotation(model.commandType().getSimpleName().toString())
                .ifPresent(controller::addAnnotation);
        model.operations().stream()
                .filter(ViewCommandWriter::isPublic)
                .forEach(operation -> controller.addMethod(controllerMethod(operation)));
        new JavaFileWriter(context.processingEnvironment(), "infrastructure.http")
                .writeFile(packageName(model.commandType()), controllerType.simpleName(), controller.build());
    }

    private static MethodSpec controllerMethod(CommandOperation operation) {
        var method = MethodSpec.methodBuilder(operation.method().getSimpleName().toString())
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(operation.httpMethod(), operation.path()));
        var sourceReturnType = TypeName.get(operation.method().getReturnType());
        var isVoid = operation.method().getReturnType().toString().equals("void");
        method.returns(ParameterizedTypeName.get(
                ClassName.get(ResponseEntity.class), isVoid ? ClassName.get(Void.class) : sourceReturnType));
        operation.method().getParameters().forEach(parameter -> {
            var name = parameter.getSimpleName().toString();
            var parameterSpec = ParameterSpec.builder(TypeName.get(parameter.asType()), name);
            if (operation.pathVariables().contains(name)) {
                parameterSpec.addAnnotation(AnnotationSpec.builder(PathVariable.class)
                        .addMember("value", "$S", name)
                        .build());
                ControllerUtil.pathParameterAnnotation("Command path parameter " + name)
                        .ifPresent(parameterSpec::addAnnotation);
            } else if (operation.bodyParameters().contains(name)) {
                parameterSpec.addAnnotation(RequestBody.class);
                parameterSpec.addAnnotation(Valid.class);
            }
            method.addParameter(parameterSpec.build());
        });
        var args = operation.method().getParameters().stream()
                .map(parameter -> parameter.getSimpleName().toString())
                .collect(java.util.stream.Collectors.joining(", "));
        if (isVoid) {
            method.addStatement("service.$N($L)", operation.method().getSimpleName(), args)
                    .addStatement("return $T.ok().build()", ResponseEntity.class);
        } else {
            method.addStatement("return $T.ok(service.$N($L))", ResponseEntity.class,
                    operation.method().getSimpleName(), args);
        }
        method.addExceptions(operation.method().getThrownTypes().stream().map(TypeName::get).toList());
        ControllerUtil.operationAnnotation(operation.method().getSimpleName().toString())
                .ifPresent(method::addAnnotation);
        ControllerUtil.securedAnnotation(operation.security()).ifPresent(method::addAnnotation);
        return method.build();
    }

    private static AnnotationSpec requestMapping(String method, String path) {
        return AnnotationSpec.builder(RequestMapping.class)
                .addMember("method", "$T.$N", RequestMethod.class, method)
                .addMember("path", "$S", path)
                .build();
    }

    static String basePath(TypeElement commandType) {
        var plural = org.javalite.common.Inflector.pluralize(commandType.getSimpleName().toString());
        return "/" + toKebabCase(plural);
    }

    static boolean isPublic(CommandOperation operation) {
        return operation.exposure() != Exposure.INTERNAL;
    }

    private static String packageName(TypeElement type) {
        return type.getEnclosingElement() instanceof javax.lang.model.element.PackageElement packageElement
                ? packageElement.getQualifiedName().toString()
                : "";
    }

    record CommandModel(TypeElement commandType, TypeElement aggregateType, List<CommandOperation> operations) {
    }

    record CommandOperation(
            ExecutableElement method,
            String httpMethod,
            String path,
            Security security,
            Exposure exposure,
            Set<String> pathVariables,
            Set<String> bodyParameters,
            String mapping
    ) {
    }
}
