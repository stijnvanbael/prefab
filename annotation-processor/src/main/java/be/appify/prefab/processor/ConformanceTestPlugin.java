package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Exposure;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.processor.event.EventBindings;
import be.appify.prefab.processor.rest.ControllerUtil;
import be.appify.prefab.processor.rest.ExposureResolver;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeName;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Generates fast integration checks for security and event serialization wiring.
 */
public final class ConformanceTestPlugin implements PrefabPlugin {
    private static final ClassName INTEGRATION_TEST =
            ClassName.get("be.appify.prefab.test", "IntegrationTest");
    private static final ClassName TEST_PROPERTY_SOURCE =
            ClassName.get("org.springframework.test.context", "TestPropertySource");
    private static final ClassName ASSERTIONS = ClassName.get("org.assertj.core.api", "Assertions");
    private static final ClassName JUNIT_TEST = ClassName.get("org.junit.jupiter.api", "Test");
    private static final ClassName MOCK_MVC = ClassName.get("org.springframework.test.web.servlet", "MockMvc");
    private static final ClassName MOCK_MVC_REQUEST_BUILDERS =
            ClassName.get("org.springframework.test.web.servlet.request", "MockMvcRequestBuilders");
    private static final ClassName MESSAGE_CODEC =
            ClassName.get("be.appify.prefab.test.inmemory.messaging", "MessageCodec");
    private static final ClassName SECURITY_REQUEST_POST_PROCESSORS =
            ClassName.get("org.springframework.security.test.web.servlet.request", "SecurityMockMvcRequestPostProcessors");
    private static final ClassName SIMPLE_GRANTED_AUTHORITY =
            ClassName.get("org.springframework.security.core.authority", "SimpleGrantedAuthority");

    private PrefabContext context;
    private TestJavaFileWriter writer;
    private final java.util.Set<String> writtenTests = new java.util.HashSet<>();

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        writer = new TestJavaFileWriter(context, null);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        if (context.processingEnvironment().getElementUtils().getTypeElement(INTEGRATION_TEST.canonicalName()) == null
                && writer.getRootPath().isEmpty()) {
            return;
        }
        manifests.stream()
                .filter(ClassManifest::isAggregate)
                .filter(manifest -> manifest.type().asElement().getAnnotation(Aggregate.class).conformanceTest())
                .forEach(this::writeSecurityTest);
        context.eventElements().forEach(this::writeEventTests);
    }

    private void writeSecurityTest(ClassManifest manifest) {
        if (!ControllerUtil.SECURITY_INCLUDED || !writtenTests.add(manifest.qualifiedName() + ":security")) {
            return;
        }
        var typeElement = (TypeElement) manifest.element();
        var endpoints = endpoints(manifest);
        if (endpoints.isEmpty()) {
            return;
        }
        var testName = manifest.simpleName() + "SecurityConformanceTest";
        var type = TypeSpec.classBuilder(testName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(INTEGRATION_TEST)
                .addAnnotation(inMemoryTestProperty())
                .addField(FieldSpec.builder(MOCK_MVC, "mockMvc", Modifier.PRIVATE)
                        .addAnnotation(Autowired.class)
                        .build());
        for (var index = 0; index < endpoints.size(); index++) {
            type.addMethod(securityTest(endpoints.get(index), index));
        }
        type.addMethod(securityAssertionHelper());
        writer.setPreferredElement(typeElement);
        writer.writeFile(manifest.packageName(), testName, type.build());
    }

    private List<Endpoint> endpoints(ClassManifest manifest) {
        var type = (TypeElement) manifest.element();
        var basePath = "/" + ControllerUtil.pathOf(manifest);
        var endpoints = new ArrayList<Endpoint>();

        manifest.constructorsWith(Create.class).forEach(constructor -> {
            var create = constructor.getAnnotation(Create.class);
            addEndpoint(endpoints, type, constructor, create.exposure(), create.security(), create.method(),
                    basePath + create.path());
        });
        manifest.staticMethodsWith(Create.class).forEach(factory -> {
            var create = factory.getAnnotation(Create.class);
            addEndpoint(endpoints, type, factory, create.exposure(), create.security(), create.method(),
                    basePath + create.path());
        });
        var getById = type.getAnnotation(GetById.class);
        if (getById != null) {
            addEndpoint(endpoints, type, type, getById.exposure(), getById.security(), getById.method(),
                    basePath + getById.path());
        }
        var getList = type.getAnnotation(GetList.class);
        if (getList != null) {
            addEndpoint(endpoints, type, type, getList.exposure(), getList.security(), getList.method(),
                    basePath + getList.path());
        }
        manifest.methodsWith(Update.class).forEach(method -> {
            var update = method.getAnnotation(Update.class);
            addEndpoint(endpoints, type, method, update.exposure(), update.security(), update.method(),
                    basePath + "/{id}" + update.path());
        });
        var typeDelete = type.getAnnotation(Delete.class);
        if (typeDelete != null) {
            addEndpoint(endpoints, type, type, typeDelete.exposure(), typeDelete.security(), typeDelete.method(),
                    basePath + typeDelete.path());
        }
        manifest.methodsWith(Delete.class).forEach(method -> {
            var delete = method.getAnnotation(Delete.class);
            addEndpoint(endpoints, type, method, delete.exposure(), delete.security(), delete.method(),
                    basePath + delete.path());
        });
        return List.copyOf(endpoints);
    }

    private void addEndpoint(
            List<Endpoint> endpoints,
            TypeElement aggregate,
            Element element,
            Exposure exposure,
            Security security,
            String method,
            String path
    ) {
        if (ExposureResolver.isPublic(aggregate, exposure)) {
            var name = element instanceof ExecutableElement executable
                    && executable.getKind() != javax.lang.model.element.ElementKind.CONSTRUCTOR
                    ? executable.getSimpleName().toString()
                    : method.toLowerCase(Locale.ROOT);
            endpoints.add(new Endpoint(name, method, path.replaceAll("\\{[^}]+}", "conformance-test-id"), security));
        }
    }

    private MethodSpec securityTest(Endpoint endpoint, int index) {
        var testName = "checks" + capitalize(endpoint.name()) + "EndpointSecurity" + index;
        return MethodSpec.methodBuilder(testName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JUNIT_TEST)
                .addException(Exception.class)
                .addStatement("assertEndpointSecurity($S, $S, $L, $S, $S)",
                        endpoint.method(), endpoint.path(), !endpoint.security().enabled(),
                        endpoint.security().role(), endpoint.security().authority())
                .build();
    }

    private MethodSpec securityAssertionHelper() {
        var method = MethodSpec.methodBuilder("assertEndpointSecurity")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(String.class, "httpMethod")
                .addParameter(String.class, "path")
                .addParameter(boolean.class, "anonymousAllowed")
                .addParameter(String.class, "role")
                .addParameter(String.class, "authority")
                .addException(Exception.class)
                .addStatement("var request = $T.request($T.valueOf(httpMethod), path)",
                        MOCK_MVC_REQUEST_BUILDERS, HttpMethod.class)
                .addStatement("var anonymousStatus = mockMvc.perform(request).andReturn().getResponse().getStatus()")
                .beginControlFlow("if (anonymousAllowed)")
                .addStatement("$T.assertThat(anonymousStatus).as($S, httpMethod, path).isNotIn(401, 403)",
                        ASSERTIONS,
                        "Anonymous request to %s %s was declared public; a competing SecurityFilterChain may be blocking it")
                .nextControlFlow("else")
                .addStatement("$T.assertThat(anonymousStatus).as($S, httpMethod, path).isIn(401, 403)",
                        ASSERTIONS,
                        "Anonymous request to secured endpoint %s %s should be rejected")
                .endControlFlow()
                .addStatement("var user = $T.user($S)", SECURITY_REQUEST_POST_PROCESSORS, "prefab-conformance");
        method.beginControlFlow("if (!authority.isBlank())")
                .addStatement("user.authorities(new $T(authority))", SIMPLE_GRANTED_AUTHORITY)
                .nextControlFlow("else if (!role.isBlank())")
                .addStatement("user.roles(role)")
                .endControlFlow()
                .addStatement("var authenticatedRequest = $T.request($T.valueOf(httpMethod), path).with(user)",
                        MOCK_MVC_REQUEST_BUILDERS, HttpMethod.class)
                .addStatement("var authenticatedStatus = mockMvc.perform(authenticatedRequest).andReturn().getResponse().getStatus()")
                .addStatement("$T.assertThat(authenticatedStatus).as($S, httpMethod, path).isNotIn(401, 403)",
                        ASSERTIONS,
                        "Authenticated request to %s %s should reach the generated endpoint; Prefab's SecurityFilterChain may have been replaced");
        return method.build();
    }

    private void writeEventTests(TypeElement eventElement) {
        var bindings = EventBindings.typesOf(eventElement, context).toList();
        for (var binding : bindings) {
            var event = EventBindings.annotationFor(binding);
            if (!event.conformanceTest()) {
                continue;
            }
            var payloadTypes = binding.isSealed()
                    ? binding.permittedSubtypes().stream().filter(TypeManifest::isRecord).toList()
                    : binding.isRecord() ? List.of(binding) : List.<TypeManifest>of();
            if (payloadTypes.isEmpty()) {
                continue;
            }
            var identity = binding.asTypeName().toString() + ":events";
            if (!writtenTests.add(identity)) {
                continue;
            }
            writeEventTest(binding, event, payloadTypes, eventElement);
        }
    }

    private void writeEventTest(
            TypeManifest binding,
            Event event,
            List<TypeManifest> payloadTypes,
            TypeElement originatingElement
    ) {
        var testName = binding.simpleName().replace(".", "") + EventBindings.suffix(binding)
                + "EventConformanceTest";
        var type = TypeSpec.classBuilder(testName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(INTEGRATION_TEST)
                .addAnnotation(inMemoryTestProperty())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), MESSAGE_CODEC),
                                "codecs", Modifier.PRIVATE)
                        .addAnnotation(Autowired.class)
                        .build());
        int index = 0;
        for (var payloadType : payloadTypes) {
            for (var topic : event.topic()) {
                type.addMethod(eventRoundTripTest(binding, payloadType, topic, index++));
            }
        }
        writer.setPreferredElement(originatingElement);
        writer.writeFile(binding.packageName(), testName, type.build());
    }

    private static AnnotationSpec inMemoryTestProperty() {
        return AnnotationSpec.builder(TEST_PROPERTY_SOURCE)
                .addMember("properties", "$S", "prefab.test.containers=false")
                .build();
    }

    private MethodSpec eventRoundTripTest(TypeManifest binding, TypeManifest payloadType, String topic, int index) {
        var simpleName = payloadType.simpleName().replace(".", "");
        var eventMotherName = simpleName + EventBindings.suffix(payloadType) + "Mother";
        var eventTypeName = typeName(payloadType);
        var motherName = ClassName.get(payloadType.packageName(), eventMotherName);
        var factory = "create" + capitalize(simpleName);
        var type = typeName(binding);
        var expected = CodeBlock.of("$T.$N()", motherName, factory);
        var publication = binding.parameters().isEmpty()
                ? CodeBlock.of("expected")
                : CodeBlock.of("new $T<>(new $T<$T>() {}, expected)", TypedEvent.class, EventType.class, type);
        return MethodSpec.methodBuilder("roundTrips" + capitalize(simpleName) + "OnTopic" + index)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JUNIT_TEST)
                .addException(Exception.class)
                .addStatement("$T expected = $L", eventTypeName, expected)
                .addStatement("var codec = codecs.stream().filter(candidate -> candidate.supports(new $T<$T>() {}))"
                        + ".findFirst().orElseThrow()", EventType.class, type)
                .addStatement("$T actual = ($T) codec.encode($S, $L).decode()",
                        type, type, topic, publication)
                .addStatement("$T.assertThat(actual).as($S).usingRecursiveComparison()"
                                + ".withComparatorForType($T::compareTo, $T.class).isEqualTo(expected)",
                        ASSERTIONS, "Event %s should round-trip through the platform serializer on topic %s"
                                .formatted(binding.simpleName(), topic), BigDecimal.class, BigDecimal.class)
                .build();
    }

    private static TypeName typeName(TypeManifest type) {
        var typeElement = (TypeElement) type.asElement();
        var className = className(typeElement);
        if (type.parameters().isEmpty()) {
            return className;
        }
        return ParameterizedTypeName.get(className,
                type.parameters().stream().map(TypeManifest::asTypeName).toArray(TypeName[]::new));
    }

    private static ClassName className(TypeElement element) {
        if (element.getEnclosingElement() instanceof TypeElement enclosingType) {
            return className(enclosingType).nestedClass(element.getSimpleName().toString());
        }
        var packageName = element.getEnclosingElement() instanceof PackageElement packageElement
                ? packageElement.getQualifiedName().toString()
                : "";
        return ClassName.get(packageName, element.getSimpleName().toString());
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private record Endpoint(String name, String method, String path, Security security) {
    }
}
