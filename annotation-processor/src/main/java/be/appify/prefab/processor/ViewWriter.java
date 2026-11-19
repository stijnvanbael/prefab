package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.ViewOf;
import be.appify.prefab.core.tenant.TenantContextProvider;
import be.appify.prefab.core.view.ViewAssembler;
import be.appify.prefab.core.view.ViewContext;
import be.appify.prefab.processor.rest.ControllerUtil;
import be.appify.prefab.processor.rest.getlist.GetListUtil;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.text.WordUtils.capitalize;

/** Generates read controllers and services for {@link ViewOf}-annotated records. */
final class ViewWriter {
    private static final String VIEW_ASSEMBLER = ViewAssembler.class.getCanonicalName();

    private final Set<String> writtenViews = new HashSet<>();
    private final ViewCommandWriter commandWriter = new ViewCommandWriter();

    void writeViews(PrefabContext context, RoundEnvironment roundEnvironment) {
        commandWriter.writeCommands(context, roundEnvironment);
        roundEnvironment.getElementsAnnotatedWith(ViewOf.class).stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .filter(view -> writtenViews.add(view.getQualifiedName().toString()))
                .forEach(view -> writeView(context, roundEnvironment, view));
    }

    private void writeView(PrefabContext context, RoundEnvironment roundEnvironment, TypeElement viewElement) {
        if (ClassManifest.hasUnresolvedFields(viewElement)) {
            writtenViews.remove(viewElement.getQualifiedName().toString());
            return;
        }
        var processingEnvironment = context.processingEnvironment();
        var aggregateElement = aggregateType(context, viewElement);
        if (aggregateElement == null) {
            return;
        }
        if (aggregateElement.getAnnotation(Aggregate.class) == null) {
            context.logError("[@ViewOf] " + viewElement.getSimpleName()
                    + ": value must be an @Aggregate type.\n"
                    + "Suggested fix: set @ViewOf to the aggregate root class.", viewElement);
            return;
        }
        var aggregate = ClassManifest.of(aggregateElement, processingEnvironment);
        var view = ClassManifest.of(viewElement, processingEnvironment);
        if (!view.type().isRecord()) {
            context.logError("[@ViewOf] " + view.simpleName()
                    + ": views must be records.\nSuggested fix: declare the view as a record.", viewElement);
            return;
        }

        var getById = viewElement.getAnnotation(GetById.class);
        var getList = viewElement.getAnnotation(GetList.class);
        if (getById == null && getList == null) {
            var create = viewElement.getAnnotation(Create.class);
            if (create != null) {
                if (commandWriter.writeCreateForm(context, viewElement, aggregateElement, create)) {
                    writeMother(context, viewElement, view);
                }
                return;
            }
            context.logError("[@ViewOf] " + view.simpleName()
                    + ": add @GetById or @GetList to expose a read endpoint.", viewElement);
            return;
        }
        if (getById != null && aggregateElement.getAnnotation(GetById.class) == null) {
            context.logError("[@ViewOf] " + view.simpleName()
                    + ": aggregate " + aggregate.simpleName() + " needs @GetById to provide source rows.\n"
                    + "Suggested fix: add @GetById to the aggregate, using exposure = Exposure.INTERNAL if "
                    + "the aggregate must not have a public endpoint.", viewElement);
        }
        if (getList != null && aggregateElement.getAnnotation(GetList.class) == null) {
            context.logError("[@ViewOf] " + view.simpleName()
                    + ": aggregate " + aggregate.simpleName() + " needs @GetList to provide source rows.\n"
                    + "Suggested fix: add @GetList to the aggregate, using exposure = Exposure.INTERNAL if "
                    + "the aggregate must not have a public endpoint.", viewElement);
        }
        if (getList != null && aggregate.parent().isPresent()) {
            context.logError("[@ViewOf] " + view.simpleName()
                    + ": list views of child aggregates require a parent path parameter, which is not supported.\n"
                    + "Suggested fix: expose the parent-scoped query through a custom service.", viewElement);
        }
        if (getById != null && aggregateElement.getAnnotation(GetById.class) == null
                || getList != null && aggregateElement.getAnnotation(GetList.class) == null
                || getList != null && aggregate.parent().isPresent()) {
            return;
        }

        var mapping = mappingOf(context, roundEnvironment, aggregateElement, aggregate, viewElement, view);
        if (mapping.isEmpty()) {
            return;
        }
        var model = new ViewModel(aggregate, view, mapping.get(), getById, getList,
                getList == null ? List.of() : GetListUtil.filterPropertiesOf(aggregate));
        writeService(context, model);
        writeController(context, model);
        new TestClientWriter(context).writeViewTestSupport(
                viewElement, view, basePath(view), getById, getList,
                getList == null ? List.of() : GetListUtil.filterPropertiesOf(aggregate));
        writeMother(context, viewElement, view);
    }

    private TypeElement aggregateType(PrefabContext context, TypeElement viewElement) {
        var processingEnvironment = context.processingEnvironment();
        return viewElement.getAnnotationMirrors().stream()
                .filter(annotation -> annotation.getAnnotationType().toString().equals(ViewOf.class.getCanonicalName()))
                .findFirst()
                .flatMap(annotation -> processingEnvironment.getElementUtils()
                        .getElementValuesWithDefaults(annotation).entrySet().stream()
                        .filter(entry -> entry.getKey().getSimpleName().contentEquals("value"))
                        .map(entry -> processingEnvironment.getTypeUtils()
                                .asElement((TypeMirror) entry.getValue().getValue()))
                        .filter(TypeElement.class::isInstance)
                        .map(TypeElement.class::cast)
                        .findFirst())
                .orElseGet(() -> {
                    context.logError("[@ViewOf] " + viewElement.getSimpleName()
                            + ": aggregate type could not be resolved.", viewElement);
                    return null;
                });
    }

    private Optional<Mapping> mappingOf(
            PrefabContext context,
            RoundEnvironment roundEnvironment,
            TypeElement aggregateElement,
            ClassManifest aggregate,
            TypeElement viewElement,
            ClassManifest view
    ) {
        var types = context.processingEnvironment().getTypeUtils();
        var matchingFields = view.fields().stream()
                .map(viewField -> {
                    var aggregateField = aggregate.fields().stream()
                            .filter(field -> field.name().equals(viewField.name()))
                            .findFirst();
                    if (aggregateField.isEmpty()) {
                        return new FieldMismatch(viewField.name(), null, viewField.type().simpleName());
                    }
                    var sourceType = aggregateField.get().element().asType();
                    var targetType = viewField.element().asType();
                    return types.isSameType(sourceType, targetType)
                            ? null
                            : new FieldMismatch(viewField.name(), aggregateField.get().type().simpleName(),
                            viewField.type().simpleName());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        var allFieldsMatch = matchingFields.isEmpty();
        if (allFieldsMatch) {
            return Optional.of(new Mapping(MappingKind.FIELDS, List.of()));
        }
        var constructor = viewElement.getEnclosedElements().stream()
                .filter(element -> element.getKind().isExecutable())
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getKind() == javax.lang.model.element.ElementKind.CONSTRUCTOR)
                .filter(element -> element.getModifiers().contains(Modifier.PUBLIC))
                .filter(element -> element.getParameters().size() == 1)
                .filter(element -> types.isSameType(element.getParameters().getFirst().asType(), aggregateElement.asType()))
                .findFirst();
        if (constructor.isPresent()) {
            return Optional.of(new Mapping(MappingKind.CONSTRUCTOR, List.of()));
        }
        var factory = viewElement.getEnclosedElements().stream()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .filter(method -> method.getKind() == javax.lang.model.element.ElementKind.METHOD)
                .filter(method -> method.getSimpleName().contentEquals("from"))
                .filter(method -> method.getModifiers().containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC)))
                .filter(method -> method.getParameters().size() == 1)
                .filter(method -> types.isSameType(method.getParameters().getFirst().asType(), aggregateElement.asType()))
                .filter(method -> types.isSameType(method.getReturnType(), viewElement.asType()))
                .findFirst();
        if (factory.isPresent()) {
            return Optional.of(new Mapping(MappingKind.FACTORY, List.of()));
        }
        if (hasCompatibleAssembler(context, roundEnvironment, aggregateElement.asType(), viewElement.asType())) {
            return Optional.of(new Mapping(MappingKind.ASSEMBLER, List.of()));
        }

        matchingFields.forEach(mismatch -> {
            var detail = mismatch.aggregateType() == null
                    ? "no aggregate field named '" + mismatch.fieldName() + "'"
                    : "aggregate field type " + mismatch.aggregateType() + " does not match view field type "
                    + mismatch.viewType();
            context.logError("[@ViewOf] " + view.simpleName() + ": cannot map field '" + mismatch.fieldName()
                    + "' from " + aggregate.simpleName() + " (" + detail + ").\n"
                    + "Suggested fix: add a public constructor or static from(" + aggregate.simpleName()
                    + ") method, or provide a Spring bean implementing ViewAssembler<"
                    + aggregate.simpleName() + ", " + view.simpleName() + ">.", viewElement);
        });
        return Optional.empty();
    }

    private boolean hasCompatibleAssembler(
            PrefabContext context,
            RoundEnvironment roundEnvironment,
            TypeMirror aggregateType,
            TypeMirror viewType
    ) {
        var types = context.processingEnvironment().getTypeUtils();
        return roundEnvironment.getRootElements().stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .anyMatch(candidate -> implementsAssembler(candidate.asType(), aggregateType, viewType, types, new HashSet<>()));
    }

    private boolean implementsAssembler(
            TypeMirror candidate,
            TypeMirror aggregateType,
            TypeMirror viewType,
            javax.lang.model.util.Types types,
            Set<String> visited
    ) {
        if (!(candidate instanceof DeclaredType declared) || !visited.add(candidate.toString())) {
            return false;
        }
        var candidateElement = (TypeElement) declared.asElement();
        if (candidateElement.getQualifiedName().contentEquals(VIEW_ASSEMBLER)) {
            return declared.getTypeArguments().size() == 2
                    && types.isSameType(declared.getTypeArguments().getFirst(), aggregateType)
                    && types.isSameType(declared.getTypeArguments().get(1), viewType);
        }
        return types.directSupertypes(declared).stream()
                .anyMatch(parent -> implementsAssembler(parent, aggregateType, viewType, types, visited));
    }

    private void writeService(PrefabContext context, ViewModel model) {
        var serviceType = ClassName.get(model.view().packageName() + ".application",
                model.view().simpleName() + "Service");
        var aggregateServiceType = ClassName.get(model.aggregate().packageName() + ".application",
                model.aggregate().simpleName() + "Service");
        var aggregateServiceField = FieldSpec.builder(aggregateServiceType,
                uncapitalize(model.aggregate().simpleName()) + "Service", PRIVATE, Modifier.FINAL).build();
        var type = TypeSpec.classBuilder(serviceType)
                .addModifiers(PUBLIC)
                .addAnnotation(Component.class)
                .addAnnotation(AnnotationSpec.builder(Transactional.class).addMember("readOnly", "true").build())
                .addField(aggregateServiceField);
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(aggregateServiceType, aggregateServiceField.name())
                .addStatement("this.$N = $N", aggregateServiceField.name(), aggregateServiceField.name());

        FieldSpec assemblerField = null;
        FieldSpec tenantProviderField = null;
        if (model.mapping().kind() == MappingKind.ASSEMBLER) {
            var assemblerType = ParameterizedTypeName.get(
                    ClassName.get(ViewAssembler.class),
                    model.aggregate().type().asTypeName(),
                    model.view().type().asTypeName());
            assemblerField = FieldSpec.builder(assemblerType, "assembler", PRIVATE, Modifier.FINAL).build();
            tenantProviderField = FieldSpec.builder(
                    ClassName.get(TenantContextProvider.class), "tenantContextProvider", PRIVATE, Modifier.FINAL).build();
            type.addField(assemblerField).addField(tenantProviderField);
            constructor.addParameter(assemblerType, "assembler")
                    .addParameter(TenantContextProvider.class, "tenantContextProvider")
                    .addStatement("this.assembler = assembler")
                    .addStatement("this.tenantContextProvider = tenantContextProvider");
        }
        type.addMethod(constructor.build());

        var viewType = model.view().type().asTypeName();
        var aggregateType = model.aggregate().type().asTypeName();
        var aggregateServiceName = aggregateServiceField.name();
        if (model.mapping().kind() != MappingKind.ASSEMBLER) {
            type.addMethod(mapMethod(model, viewType, aggregateType));
        }
        if (model.getById() != null) {
            var getById = MethodSpec.methodBuilder("getById")
                    .addModifiers(PUBLIC)
                    .addParameter(String.class, "id")
                    .addParameter(Principal.class, "principal")
                    .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), viewType));
            if (model.mapping().kind() == MappingKind.ASSEMBLER) {
                getById.addStatement("""
                        return $N.getById(id).map(aggregate -> {
                            var views = assembler.assemble($T.of(aggregate), $L);
                            if (views.size() != 1) {
                                throw new $T("ViewAssembler must return exactly one view for a single source row");
                            }
                            return views.getFirst();
                        })""", aggregateServiceName, List.class, viewContext(), IllegalStateException.class);
            } else {
                getById.addStatement("return $N.getById(id).map(this::map)", aggregateServiceName);
            }
            type.addMethod(getById.build());
        }
        if (model.getList() != null) {
            var aggregateListCall = CodeBlock.builder()
                    .add("$N.getList(pageable", aggregateServiceName)
                    .add(model.filters().stream()
                            .map(filter -> ", " + filter.parameterName())
                            .collect(java.util.stream.Collectors.joining()))
                    .add(")")
                    .build();
            var getList = MethodSpec.methodBuilder("getList")
                    .addModifiers(PUBLIC)
                    .addParameter(Pageable.class, "pageable")
                    .addParameter(Principal.class, "principal")
                    .addParameters(model.filters().stream()
                            .map(filter -> ParameterSpec.builder(
                                    GetListUtil.filterParameterType(filter), filter.parameterName()).build())
                            .toList())
                    .returns(ParameterizedTypeName.get(ClassName.get(Page.class), viewType))
                    .addStatement("var page = $L", aggregateListCall);
            if (model.mapping().kind() == MappingKind.ASSEMBLER) {
                getList.addStatement("var views = assembler.assemble(page.getContent(), $L)", viewContext())
                        .addStatement("""
                                if (views.size() != page.getNumberOfElements()) {
                                    throw new $T("ViewAssembler must return one view for each source row");
                                }""", IllegalStateException.class)
                        .addStatement("return new $T<>(views, page.getPageable(), page.getTotalElements())",
                                PageImpl.class);
            } else {
                getList.addStatement("return page.map(this::map)");
            }
            type.addMethod(getList.build());
        }
        new JavaFileWriter(context.processingEnvironment(), "application")
                .writeFile(model.view().packageName(), serviceType.simpleName(), type.build());
    }

    private static CodeBlock viewContext() {
        return CodeBlock.of("$T.of(principal, tenantContextProvider.currentTenantId())", ViewContext.class);
    }

    private static MethodSpec mapMethod(ViewModel model, TypeName viewType, TypeName aggregateType) {
        var mapping = model.mapping();
        var map = MethodSpec.methodBuilder("map")
                .addModifiers(PRIVATE)
                .returns(viewType)
                .addParameter(aggregateType, "aggregate");
        switch (mapping.kind()) {
            case FIELDS -> map.addStatement("return new $T($L)", viewType,
                    model.view().fields().stream()
                            .map(field -> "aggregate.%s()".formatted(field.name()))
                            .collect(java.util.stream.Collectors.joining(", ")));
            case CONSTRUCTOR -> map.addStatement("return new $T(aggregate)", viewType);
            case FACTORY -> map.addStatement("return $T.from(aggregate)", viewType);
            case ASSEMBLER -> throw new IllegalStateException("Assembler mappings do not use a single-row mapper");
        }
        return map.build();
    }

    private void writeController(PrefabContext context, ViewModel model) {
        var view = model.view();
        var serviceType = ClassName.get(view.packageName() + ".application", view.simpleName() + "Service");
        var controllerType = view.simpleName() + "Controller";
        var basePath = basePath(view);
        var type = TypeSpec.classBuilder(controllerType)
                .addModifiers(PUBLIC)
                .addAnnotation(RestController.class)
                .addAnnotation(AnnotationSpec.builder(RequestMapping.class)
                        .addMember("path", "$S", basePath)
                        .build())
                .addField(serviceType, "service", PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(PUBLIC)
                        .addParameter(serviceType, "service")
                        .addStatement("this.service = service")
                        .build());
        ControllerUtil.tagAnnotation(view.simpleName()).ifPresent(type::addAnnotation);
        if (model.getById() != null) {
            var getById = model.getById();
            var idParameter = ParameterSpec.builder(String.class, "id")
                    .addAnnotation(PathVariable.class);
            ControllerUtil.pathParameterAnnotation("The " + view.simpleName() + " ID")
                    .ifPresent(idParameter::addAnnotation);
            var method = MethodSpec.methodBuilder("getById")
                    .addModifiers(PUBLIC)
                    .addAnnotation(requestMapping(getById.method(), getById.path()))
                    .returns(ParameterizedTypeName.get(
                            ClassName.get(ResponseEntity.class), view.type().asTypeName()))
                    .addParameter(idParameter.build())
                    .addParameter(Principal.class, "principal")
                    .addStatement("return service.getById(id, principal).map($T::ok).orElse($T.notFound().build())",
                            ResponseEntity.class, ResponseEntity.class);
            ControllerUtil.operationAnnotation("Get " + view.simpleName() + " by ID").ifPresent(method::addAnnotation);
            ControllerUtil.securedAnnotation(getById.security()).ifPresent(method::addAnnotation);
            type.addMethod(method.build());
        }
        if (model.getList() != null) {
            var getList = model.getList();
            var method = MethodSpec.methodBuilder("getList")
                    .addModifiers(PUBLIC)
                    .addAnnotation(requestMapping(getList.method(), getList.path()))
                    .returns(ParameterizedTypeName.get(
                            ClassName.get(ResponseEntity.class),
                            ParameterizedTypeName.get(ClassName.get(PagedModel.class), view.type().asTypeName())))
                    .addParameter(pageableParameter(getList))
                    .addParameter(Principal.class, "principal")
                    .addParameters(model.filters().stream()
                            .map(filter -> ParameterSpec.builder(
                                    GetListUtil.filterParameterType(filter), filter.parameterName()).build())
                            .toList())
                    .addStatement("return $T.ok(new $T(service.getList(pageable, principal$L)))",
                            ResponseEntity.class, PagedModel.class,
                            model.filters().stream()
                                    .map(filter -> ", " + filter.parameterName())
                                    .collect(java.util.stream.Collectors.joining()));
            ControllerUtil.operationAnnotation("List " + view.plural()).ifPresent(method::addAnnotation);
            ControllerUtil.securedAnnotation(getList.security()).ifPresent(method::addAnnotation);
            type.addMethod(method.build());
        }
        new JavaFileWriter(context.processingEnvironment(), "infrastructure.http")
                .writeFile(view.packageName(), controllerType, type.build());
    }

    private static AnnotationSpec requestMapping(String method, String path) {
        return AnnotationSpec.builder(RequestMapping.class)
                .addMember("method", "$T.$N", RequestMethod.class, method)
                .addMember("path", "$S", path)
                .build();
    }

    private static ParameterSpec pageableParameter(GetList getList) {
        var parameter = ParameterSpec.builder(Pageable.class, "pageable");
        var sortDefaults = java.util.Arrays.stream(getList.sorted())
                .map(order -> AnnotationSpec.builder(org.springframework.data.web.SortDefault.class)
                        .addMember("sort", "$S", order.property())
                        .addMember("direction", "$T.$L", org.springframework.data.domain.Sort.Direction.class,
                                order.direction() == be.appify.prefab.core.annotations.rest.SortOrder.Direction.ASCENDING
                                        ? "ASC" : "DESC")
                        .build())
                .toList();
        if (!sortDefaults.isEmpty()) {
            parameter.addAnnotation(AnnotationSpec.builder(
                            org.springframework.data.web.SortDefault.SortDefaults.class)
                    .addMember("value", "{$L}", sortDefaults.stream()
                            .map(annotation -> CodeBlock.of("$L", annotation))
                            .collect(CodeBlock.joining(", ")))
                    .build());
        }
        return parameter.build();
    }

    private static String uncapitalize(String value) {
        return org.apache.commons.text.WordUtils.uncapitalize(value);
    }

    private static String basePath(ClassManifest view) {
        return "/" + toKebabCase(view.plural());
    }

    private void writeMother(PrefabContext context, TypeElement viewElement, ClassManifest view) {
        var builderName = "Builder";
        var builder = TypeSpec.classBuilder(builderName)
                .addModifiers(PUBLIC, Modifier.STATIC);
        var build = MethodSpec.methodBuilder("build")
                .addModifiers(PUBLIC)
                .returns(view.type().asTypeName())
                .addStatement("return new $T($L)", view.type().asTypeName(),
                        view.fields().stream().map(VariableManifest::name)
                                .collect(java.util.stream.Collectors.joining(", ")));
        view.fields().forEach(field -> {
            var fieldName = field.name();
            var fieldType = field.type().asTypeName();
            builder.addField(FieldSpec.builder(fieldType, fieldName, PRIVATE)
                    .initializer(defaultValue(field))
                    .build());
            builder.addMethod(MethodSpec.methodBuilder(fieldName)
                    .addModifiers(PUBLIC)
                    .returns(ClassName.get("", builderName))
                    .addParameter(fieldType, fieldName)
                    .addStatement("this.$N = $N", fieldName, fieldName)
                    .addStatement("return this")
                    .build());
        });
        builder.addMethod(build.build());
        var mother = TypeSpec.classBuilder(view.simpleName() + "Mother")
                .addModifiers(PUBLIC)
                .addType(builder.build())
                .addMethod(MethodSpec.methodBuilder("builder")
                        .addModifiers(PUBLIC, Modifier.STATIC)
                        .returns(ClassName.get(view.packageName(), view.simpleName() + "Mother", builderName))
                        .addStatement("return new $T()", ClassName.get(
                                view.packageName(), view.simpleName() + "Mother", builderName))
                        .build())
                .addMethod(MethodSpec.methodBuilder("create" + capitalize(view.simpleName()))
                        .addModifiers(PUBLIC, Modifier.STATIC)
                        .returns(view.type().asTypeName())
                        .addStatement("return builder().build()")
                        .build());
        new OutputTargetFileOutput(context, null, be.appify.prefab.core.annotations.OutputTarget.TEST)
                .writeFile(view.packageName(), view.simpleName() + "Mother", mother.build());
    }

    private static CodeBlock defaultValue(VariableManifest field) {
        var type = field.type();
        if (type.is(String.class)) {
            return CodeBlock.of("$S", field.name());
        }
        if (type.is(boolean.class) || type.is(Boolean.class)) {
            return CodeBlock.of("true");
        }
        if (type.is(char.class) || type.is(Character.class)) {
            return CodeBlock.of("'a'");
        }
        if (type.is(byte.class) || type.is(Byte.class)) {
            return CodeBlock.of("(byte) 1");
        }
        if (type.is(short.class) || type.is(Short.class)) {
            return CodeBlock.of("(short) 1");
        }
        if (type.is(int.class) || type.is(Integer.class)) {
            return CodeBlock.of("1");
        }
        if (type.is(long.class) || type.is(Long.class)) {
            return CodeBlock.of("1L");
        }
        if (type.is(float.class) || type.is(Float.class)) {
            return CodeBlock.of("1.0f");
        }
        if (type.is(double.class) || type.is(Double.class)) {
            return CodeBlock.of("1.0");
        }
        if (type.is(List.class)) {
            return CodeBlock.of("$T.of()", List.class);
        }
        if (type.is(Optional.class)) {
            return CodeBlock.of("$T.empty()", Optional.class);
        }
        return CodeBlock.of("null");
    }

    private record ViewModel(
            ClassManifest aggregate,
            ClassManifest view,
            Mapping mapping,
            GetById getById,
            GetList getList,
            List<GetListUtil.FilterManifest> filters
    ) {
    }

    private record Mapping(MappingKind kind, List<String> fieldNames) {
    }

    private record FieldMismatch(String fieldName, String aggregateType, String viewType) {
    }

    private enum MappingKind {
        FIELDS,
        CONSTRUCTOR,
        FACTORY,
        ASSEMBLER
    }
}
