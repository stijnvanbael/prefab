package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Compatibility;
import be.appify.prefab.core.annotations.Default;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventMigration;
import be.appify.prefab.core.annotations.PreviousName;
import be.appify.prefab.core.annotations.Versioned;
import be.appify.prefab.core.versioning.EventVersion;
import be.appify.prefab.core.versioning.Migration;
import be.appify.prefab.processor.PrefabContext;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.StandardLocation;

/**
 * Validates the versioning of events and versioned components: the declared versions, the chains of migrations, and
 * the changes of the contract against the committed baseline according to the compatibility mode.
 *
 * <p>The baseline of a type is the file {@code prefab/event-baseline/<type>.properties} on the classpath of the
 * module (typically {@code src/main/resources}), or in the directory of the {@value #BASELINE_DIR_OPTION} option. A
 * type without baseline is not checked; the processor writes the current contract so it can be committed.
 */
class EventVersioningValidator {
    static final String BASELINE_DIR_OPTION = "prefab.events.baselineDir";
    static final String COMPATIBILITY_OPTION = "prefab.events.compatibility";
    private static final String BASELINE_PATH = "prefab/event-baseline/";
    private static final String CANDIDATE_PATH = "prefab/event-baseline-generated/";

    private final PrefabContext context;
    private final Set<String> validated = new HashSet<>();
    private final Map<String, List<MigrationDeclaration>> chains = new LinkedHashMap<>();

    EventVersioningValidator(PrefabContext context) {
        this.context = context;
    }

    private record MigrationDeclaration(TypeElement element, String oldType, String newType, int from, int to) {
    }

    void validate() {
        var roundEnvironment = context.roundEnvironment();
        var migrations = ElementFilter.typesIn(roundEnvironment.getElementsAnnotatedWith(EventMigration.class)).stream()
                .flatMap(element -> declarationOf(element).stream())
                .toList();
        validateChains(migrations);
        var versioned = new java.util.LinkedHashSet<TypeElement>();
        versioned.addAll(ElementFilter.typesIn(roundEnvironment.getElementsAnnotatedWith(Event.class)));
        versioned.addAll(ElementFilter.typesIn(roundEnvironment.getElementsAnnotatedWith(Versioned.class)));
        versioned.stream().filter(type -> validated.add(type.getQualifiedName().toString())).forEach(this::validateType);
    }

    private Optional<MigrationDeclaration> declarationOf(TypeElement element) {
        var annotation = element.getAnnotation(EventMigration.class);
        var migrationType = element.getInterfaces().stream()
                .filter(DeclaredType.class::isInstance).map(DeclaredType.class::cast)
                .filter(type -> ((TypeElement) type.asElement()).getQualifiedName().contentEquals(Migration.class.getName()))
                .findFirst();
        if (migrationType.isEmpty() || migrationType.get().getTypeArguments().size() != 2) {
            context.logError("@EventMigration class '%s' must implement Migration<Old, New>."
                    .formatted(element.getSimpleName()), element);
            return Optional.empty();
        }
        var arguments = migrationType.get().getTypeArguments();
        try {
            var from = EventVersion.parse(annotation.from()).major();
            var to = EventVersion.parse(annotation.to()).major();
            if (from >= to) {
                context.logError("@EventMigration on '%s' must migrate to a higher major version than it reads (from=%d, to=%d)."
                        .formatted(element.getSimpleName(), from, to), element);
                return Optional.empty();
            }
            return Optional.of(new MigrationDeclaration(element, nameOf(arguments.get(0)), nameOf(arguments.get(1)), from, to));
        } catch (IllegalArgumentException e) {
            context.logError("@EventMigration on '%s' has an invalid version: %s"
                    .formatted(element.getSimpleName(), e.getMessage()), element);
            return Optional.empty();
        }
    }

    private static String nameOf(TypeMirror type) {
        return type instanceof DeclaredType declared
                ? ((TypeElement) declared.asElement()).getQualifiedName().toString()
                : type.toString();
    }

    private void validateChains(List<MigrationDeclaration> migrations) {
        var byNewType = new HashMap<String, MigrationDeclaration>();
        for (var migration : migrations) {
            var duplicate = byNewType.put(migration.newType(), migration);
            if (duplicate != null) {
                context.logError("Multiple migrations produce '%s'.".formatted(migration.newType()), migration.element());
            }
        }
        var oldTypes = migrations.stream().map(MigrationDeclaration::oldType).toList();
        byNewType.keySet().stream().filter(type -> !oldTypes.contains(type)).forEach(tip -> {
            var chain = chainEndingAt(tip, byNewType);
            chains.put(tip, chain);
            validateChain(tip, chain);
        });
    }

    private static List<MigrationDeclaration> chainEndingAt(String tip, Map<String, MigrationDeclaration> byNewType) {
        var chain = new ArrayList<MigrationDeclaration>();
        var seen = new HashSet<String>();
        for (var step = byNewType.get(tip); step != null && seen.add(step.newType()); step = byNewType.get(step.oldType())) {
            chain.addFirst(step);
        }
        return chain;
    }

    private void validateChain(String tip, List<MigrationDeclaration> chain) {
        for (var i = 1; i < chain.size(); i++) {
            if (chain.get(i - 1).to() != chain.get(i).from()) {
                context.logError("Migrations of '%s' are not contiguous: version %d is followed by a migration from version %d."
                        .formatted(tip, chain.get(i - 1).to(), chain.get(i).from()), chain.get(i).element());
            }
        }
        var tipElement = context.processingEnvironment().getElementUtils().getTypeElement(tip);
        var last = chain.getLast();
        if (tipElement == null) {
            return;
        }
        var declared = declaredVersion(tipElement);
        if (declared.isEmpty() || declared.get().major() != last.to()) {
            context.logError("Migrations of '%s' end at major version %d, but the type declares version %s."
                    .formatted(tip, last.to(), declared.map(EventVersion::toString).orElse("1.0.0")), last.element());
        }
    }

    private void validateType(TypeElement type) {
        var current = declaredVersion(type);
        if (current.isEmpty()) {
            return;
        }
        var event = type.getAnnotation(Event.class);
        if (event != null) {
            validateMinSupported(type, event, current.get());
        }
        var baseline = readBaseline(type);
        var contract = new EventContract(current.get(), fieldsOf(type, ""));
        if (baseline.isEmpty()) {
            writeBaseline(type, contract, true);
            return;
        }
        var errors = new ArrayList<String>();
        compare(type, contract, baseline.get(), compatibilityOf(event), errors);
        errors.forEach(message -> context.logError(message, type));
        if (errors.isEmpty()) {
            writeBaseline(type, contract, false);
        }
    }

    private void validateMinSupported(TypeElement type, Event event, EventVersion current) {
        if (event.minSupportedVersion().isBlank()) {
            return;
        }
        try {
            var minimum = EventVersion.parse(event.minSupportedVersion());
            if (minimum.compareTo(current) > 0) {
                context.logError("minSupportedVersion %s of '%s' is higher than its version %s."
                        .formatted(minimum, type.getSimpleName(), current), type);
            }
        } catch (IllegalArgumentException e) {
            context.logError("Invalid minSupportedVersion on '%s': %s".formatted(type.getSimpleName(), e.getMessage()), type);
        }
    }

    private void compare(
            TypeElement type,
            EventContract current,
            EventContract baseline,
            Compatibility mode,
            List<String> errors
    ) {
        var name = type.getSimpleName();
        if (current.version().compareTo(baseline.version()) < 0) {
            errors.add("Version %s of '%s' is lower than the released version %s; versions must not decrease."
                    .formatted(current.version(), name, baseline.version()));
            return;
        }
        if (current.version().major() > baseline.version().major()) {
            validateMigrationCoverage(type, baseline.version(), current.version(), errors);
            return;
        }
        var changes = current.changesSince(baseline, renamedFields(type));
        var violations = changes.violations(mode);
        if (!violations.isEmpty()) {
            errors.add("Breaking change of '%s' (compatibility %s) requires a major version bump from %s with a migration: %s"
                    .formatted(name, mode, baseline.version(), String.join("; ", violations)));
        } else if (changes.changed() && current.version().minor() == baseline.version().minor()) {
            errors.add("The contract of '%s' changed since version %s; bump at least the minor version."
                    .formatted(name, baseline.version()));
        }
    }

    private void validateMigrationCoverage(
            TypeElement type,
            EventVersion baseline,
            EventVersion current,
            List<String> errors
    ) {
        var chain = chains.get(type.getQualifiedName().toString());
        var covered = chain != null && chain.stream().anyMatch(step -> step.from() == baseline.major())
                && chain.getLast().to() == current.major();
        if (!covered) {
            errors.add("Major version bump of '%s' from %s to %s requires a @EventMigration chain from major version %d to %d."
                    .formatted(type.getSimpleName(), baseline, current, baseline.major(), current.major()));
        }
    }

    private Compatibility compatibilityOf(Event event) {
        if (event != null && event.compatibility() != Compatibility.DEFAULT) {
            return event.compatibility();
        }
        var configured = context.processingEnvironment().getOptions().get(COMPATIBILITY_OPTION);
        if (configured == null || configured.isBlank()) {
            return Compatibility.BACKWARD;
        }
        try {
            var mode = Compatibility.valueOf(configured.strip().toUpperCase());
            return mode == Compatibility.DEFAULT ? Compatibility.BACKWARD : mode;
        } catch (IllegalArgumentException e) {
            context.processingEnvironment().getMessager().printMessage(javax.tools.Diagnostic.Kind.WARNING,
                    "Unknown value '%s' of -A%s, using BACKWARD.".formatted(configured, COMPATIBILITY_OPTION));
            return Compatibility.BACKWARD;
        }
    }

    private Optional<EventVersion> declaredVersion(TypeElement type) {
        try {
            var event = type.getAnnotation(Event.class);
            if (event != null) {
                return Optional.of(EventVersion.parse(event.version()));
            }
            var versioned = type.getAnnotation(Versioned.class);
            if (versioned != null) {
                return Optional.of(EventVersion.parse(versioned.version()));
            }
        } catch (IllegalArgumentException e) {
            context.logError("Invalid version on '%s': %s".formatted(type.getSimpleName(), e.getMessage()), type);
        }
        return Optional.empty();
    }

    private Map<String, EventContract.Field> fieldsOf(TypeElement type, String prefix) {
        var fields = new LinkedHashMap<String, EventContract.Field>();
        for (var component : type.getRecordComponents()) {
            fields.put(prefix + component.getSimpleName(), new EventContract.Field(
                    component.asType().toString(), isOptional(type, component)));
        }
        type.getPermittedSubclasses().stream().map(mirror -> ((DeclaredType) mirror).asElement())
                .map(TypeElement.class::cast)
                .forEach(subtype -> fields.putAll(fieldsOf(subtype, prefix + subtype.getSimpleName() + ".")));
        return fields;
    }

    private boolean isOptional(TypeElement type, RecordComponentElement component) {
        return component.asType().toString().startsWith("java.util.Optional<")
                || annotationOf(type, component, Default.class).isPresent()
                || Stream.of(component, fieldOf(type, component)).filter(java.util.Objects::nonNull)
                .flatMap(element -> element.getAnnotationMirrors().stream())
                .anyMatch(mirror -> mirror.getAnnotationType().asElement().getSimpleName().contentEquals("Nullable"));
    }

    private Map<String, List<String>> renamedFields(TypeElement type) {
        var renamed = new LinkedHashMap<String, List<String>>();
        collectRenames(type, "", renamed);
        return renamed;
    }

    private void collectRenames(TypeElement type, String prefix, Map<String, List<String>> renamed) {
        for (var component : type.getRecordComponents()) {
            annotationOf(type, component, PreviousName.class).ifPresent(annotation -> renamed.put(
                    prefix + component.getSimpleName(),
                    Arrays.stream(annotation.value()).map(previous -> prefix + previous).toList()));
        }
        type.getPermittedSubclasses().stream().map(mirror -> (TypeElement) ((DeclaredType) mirror).asElement())
                .forEach(subtype -> collectRenames(subtype, prefix + subtype.getSimpleName() + ".", renamed));
    }

    private static <A extends Annotation> Optional<A> annotationOf(
            TypeElement type,
            RecordComponentElement component,
            Class<A> annotation
    ) {
        return Optional.ofNullable(component.getAnnotation(annotation))
                .or(() -> Optional.ofNullable(fieldOf(type, component)).map(field -> field.getAnnotation(annotation)));
    }

    private static Element fieldOf(TypeElement type, RecordComponentElement component) {
        return ElementFilter.fieldsIn(type.getEnclosedElements()).stream()
                .filter(field -> field.getSimpleName().equals(component.getSimpleName()))
                .findFirst()
                .orElse(null);
    }

    private Optional<EventContract> readBaseline(TypeElement type) {
        var fileName = type.getQualifiedName() + ".properties";
        try {
            var directory = baselineDirectory();
            if (directory.isPresent()) {
                var file = directory.get().resolve(fileName);
                if (!Files.exists(file)) {
                    return Optional.empty();
                }
                try (var input = Files.newInputStream(file)) {
                    return Optional.of(EventContract.read(input));
                }
            }
            var resource = context.processingEnvironment().getFiler()
                    .getResource(StandardLocation.CLASS_OUTPUT, "", BASELINE_PATH + fileName);
            try (var input = resource.openInputStream()) {
                return Optional.of(EventContract.read(input));
            }
        } catch (IOException e) {
            return Optional.empty();
        } catch (RuntimeException e) {
            context.logError("Cannot read the baseline of '%s': %s".formatted(type.getSimpleName(), e.getMessage()), type);
            return Optional.empty();
        }
    }

    private void writeBaseline(TypeElement type, EventContract contract, boolean missing) {
        var fileName = type.getQualifiedName() + ".properties";
        var text = new StringWriter();
        try {
            contract.write(text);
            var directory = baselineDirectory();
            if (directory.isPresent()) {
                Files.createDirectories(directory.get());
                var file = directory.get().resolve(fileName);
                if (!Files.exists(file) || !Files.readString(file, StandardCharsets.UTF_8).equals(text.toString())) {
                    Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
                }
                return;
            }
            var current = readBaseline(type);
            if (!missing && current.isPresent() && sameContract(current.get(), contract)) {
                return;
            }
            var resource = context.processingEnvironment().getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", CANDIDATE_PATH + fileName);
            try (var writer = resource.openWriter()) {
                writer.write(text.toString());
            }
            context.logNote(("The contract of '%s' %s. Commit %s%s to src/main/resources/%s to release it as the "
                    + "baseline.").formatted(type.getSimpleName(), missing ? "has no baseline" : "changed",
                    CANDIDATE_PATH, fileName, BASELINE_PATH), type);
        } catch (IOException e) {
            context.logNote("Could not write the baseline of '%s': %s".formatted(type.getSimpleName(), e.getMessage()), type);
        }
    }

    private static boolean sameContract(EventContract left, EventContract right) {
        return left.version().equals(right.version()) && left.fields().equals(right.fields());
    }

    private Optional<Path> baselineDirectory() {
        return Optional.ofNullable(context.processingEnvironment().getOptions().get(BASELINE_DIR_OPTION))
                .filter(value -> !value.isBlank()).map(Path::of);
    }
}
