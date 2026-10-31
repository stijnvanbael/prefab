package be.appify.prefab.core.versioning;

import be.appify.prefab.core.annotations.EventMigration;
import be.appify.prefab.core.domain.EventType;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Routes messages to the version of the contract the consumer understands and migrates older messages with the
 * registered {@link Migration migrations}.
 *
 * <p>The migrations of one type form a chain per major version (1 → 2 → 3). A message written with an older major is
 * bound to the snapshot type the chain starts with and migrated step by step. A message written with a higher major,
 * or with a major for which the chain no longer exists, is rejected with an {@link UnsupportedEventVersionException}.
 * Migrations of nested {@link be.appify.prefab.core.annotations.Versioned} components are applied wherever the
 * component occurs, before the migration of the enclosing event.
 */
public final class EventMigrator {
    private static final EventMigrator NONE = new EventMigrator(List.of());

    private final Map<String, Chain> chains;
    private final Map<String, ResolvableType> chainTypes;
    private final Map<String, Class<?>> typeAliases;

    /**
     * Creates a migrator for the given migrations.
     *
     * @param migrations the migrations, each annotated with {@link EventMigration}
     * @throws IllegalStateException if the migrations do not form valid chains
     */
    public EventMigrator(Collection<? extends Migration<?, ?>> migrations) {
        var steps = migrations.stream().map(EventMigrator::stepOf).toList();
        this.chains = buildChains(steps);
        this.chainTypes = steps.stream().collect(LinkedHashMap::new,
                (map, step) -> map.put(step.newType().toString(), step.newType()), Map::putAll);
        this.typeAliases = steps.stream()
                .filter(step -> !step.fromType().isBlank())
                .filter(step -> !step.newType().hasGenerics())
                .collect(LinkedHashMap::new, (map, step) -> map.put(step.fromType(), tipOf(step).resolve()), Map::putAll);
    }

    /**
     * Returns a migrator without migrations. It accepts the current major version only.
     *
     * @return a migrator without migrations
     */
    public static EventMigrator none() {
        return NONE;
    }

    /**
     * The previous type names declared with {@link EventMigration#fromType()}.
     *
     * @return the current type by previous type name
     */
    public Map<String, Class<?>> typeAliases() {
        return Collections.unmodifiableMap(typeAliases);
    }

    /**
     * Decides how a message of the given versions is read.
     *
     * @param eventType the concrete event type the consumer reads
     * @param versions  the versions the message was written with
     * @return the plan to read the message
     * @throws UnsupportedEventVersionException if the message cannot be read by this consumer
     */
    public Plan plan(Class<?> eventType, MessageVersions versions) {
        var current = EventVersions.current(eventType);
        var produced = versions.event();
        EventVersions.minSupported(eventType).filter(minimum -> produced.major() < minimum.major()).ifPresent(minimum -> {
            throw new UnsupportedEventVersionException(
                    "Event %s version %s is older than the minimum supported version %s"
                            .formatted(eventType.getName(), produced, minimum));
        });
        var steps = stepsFor(eventType, produced, current);
        var structure = steps.isEmpty() ? ResolvableType.forClass(eventType) : steps.getFirst().oldType();
        return new Plan(structure, steps, versions, requiresComponentMigration(structure, versions));
    }

    /**
     * Plans migration for one concrete binding, never the erased generic event class.
     * @param eventType concrete binding
     * @param versions message contract versions
     * @return the binding-specific read plan
     */
    public Plan plan(EventType<?> eventType, MessageVersions versions) {
        var declaration = EventVersions.declarationFor(eventType);
        var produced = versions.event();
        if (!declaration.minSupportedVersion().isBlank()
                && produced.compareTo(EventVersion.parse(declaration.minSupportedVersion())) < 0) {
            throw new UnsupportedEventVersionException("Event " + eventType + " version " + produced
                    + " is older than minSupportedVersion " + declaration.minSupportedVersion());
        }
        var type = ResolvableType.forType(eventType.type());
        var steps = stepsFor(type, produced, EventVersion.parse(declaration.version()));
        var start = steps.isEmpty() ? type : steps.getFirst().oldType();
        return new Plan(start, steps, versions, requiresComponentMigration(start, versions));
    }

    private List<Step> stepsFor(Class<?> type, EventVersion produced, EventVersion current) {
        return stepsFor(ResolvableType.forClass(type), produced, current);
    }

    private List<Step> stepsFor(ResolvableType type, EventVersion produced, EventVersion current) {
        if (produced.major() > current.major()) {
            throw new UnsupportedEventVersionException(
                    "%s was written with version %s, which is newer than the supported major version %d of the consumer"
                            .formatted(type, produced, current.major()));
        }
        if (produced.major() == current.major()) {
            return List.of();
        }
        var chain = chains.get(type.toString());
        var index = chain == null ? -1 : chain.indexOfFrom(produced.major());
        if (index < 0) {
            throw new UnsupportedEventVersionException(
                    "%s was written with version %s, but no migration from major version %d is available"
                            .formatted(type, produced, produced.major()));
        }
        return chain.steps().subList(index, chain.steps().size());
    }

    private boolean requiresComponentMigration(ResolvableType startType, MessageVersions versions) {
        var components = referencedRecords(startType, new HashSet<>());
        return components.stream().filter(type -> type != startType.resolve(Object.class))
                .filter(type -> startType.hasGenerics() || chains.containsKey(type.getName()))
                .anyMatch(type -> versions.component(type.getName()).major() != EventVersions.current(type).major());
    }

    private Set<Class<?>> referencedRecords(ResolvableType type, Set<Class<?>> visited) {
        var element = TypeStructure.elementType(type);
        if (element.isPresent()) {
            return referencedRecords(element.get(), visited);
        }
        var raw = type.resolve(Object.class);
        var found = new LinkedHashSet<Class<?>>();
        if (!raw.isRecord() || !visited.add(raw)) {
            return found;
        }
        found.add(raw);
        for (var component : raw.getRecordComponents()) {
            found.addAll(referencedRecords(ResolvableType.forType(component.getGenericType(), type), visited));
        }
        return found;
    }

    private JsonNode migrateComponents(JsonNode node, ResolvableType type, MessageVersions versions, JsonMapper mapper) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return node;
        }
        var raw = type.resolve(Object.class);
        if (java.util.Optional.class.isAssignableFrom(raw)) {
            return migrateComponents(node, type.getGeneric(0), versions, mapper);
        }
        var element = TypeStructure.elementType(type);
        if (element.isPresent()) {
            return migrateElements(node, element.get(), versions, mapper);
        }
        var steps = componentSteps(raw, versions);
        var structureType = steps.isEmpty() ? type : steps.getFirst().oldType();
        var structure = structureType.resolve(Object.class);
        if (structure.isRecord() && !Modifier.isAbstract(structure.getModifiers()) && node instanceof ObjectNode object) {
            for (var component : structure.getRecordComponents()) {
                var child = object.get(component.getName());
                if (child != null) {
                    object.set(component.getName(),
                            migrateComponents(child, ResolvableType.forType(component.getGenericType(), structureType), versions, mapper));
                }
            }
        }
        return steps.isEmpty() ? node : mapper.valueToTree(migrate(
                mapper.readValue(node.toString(), mapper.getTypeFactory().constructType(structureType.getType())), steps));
    }

    private JsonNode migrateElements(JsonNode node, ResolvableType element, MessageVersions versions, JsonMapper mapper) {
        if (node instanceof ArrayNode array) {
            for (var i = 0; i < array.size(); i++) {
                array.set(i, migrateComponents(array.get(i), element, versions, mapper));
            }
        } else if (node instanceof ObjectNode object) {
            for (var name : new ArrayList<>(object.propertyNames())) {
                object.set(name, migrateComponents(object.get(name), element, versions, mapper));
            }
        }
        return node;
    }

    private List<Step> componentSteps(Class<?> type, MessageVersions versions) {
        try {
            return stepsFor(type, versions.component(type.getName()), EventVersions.current(type));
        } catch (UnsupportedEventVersionException e) {
            throw new UnsupportedEventVersionException("No component migration can read " + type.getName()
                    + ": " + e.getMessage());
        }
    }

    private static Object migrate(Object snapshot, List<Step> steps) {
        var value = snapshot;
        for (var step : steps) {
            value = step.migration().migrate(value);
        }
        return value;
    }

    private static Step stepOf(Migration<?, ?> migration) {
        var userClass = ClassUtils.getUserClass(migration);
        var annotation = AnnotationUtils.findAnnotation(userClass, EventMigration.class);
        if (annotation == null) {
            throw new IllegalStateException("Migration %s must be annotated with @EventMigration"
                    .formatted(userClass.getName()));
        }
        var type = ResolvableType.forClass(userClass).as(Migration.class);
        var oldType = type.getGeneric(0);
        var newType = type.getGeneric(1);
        if (oldType.resolve() == null || newType.resolve() == null || oldType.hasUnresolvableGenerics()
                || newType.hasUnresolvableGenerics()) {
            throw new IllegalStateException("Migration %s must declare its Old and New type"
                    .formatted(userClass.getName()));
        }
        var from = EventVersion.parse(annotation.from()).major();
        var to = EventVersion.parse(annotation.to()).major();
        if (from >= to) {
            throw new IllegalStateException("Migration %s must migrate to a higher major version than it reads (from=%d, to=%d)"
                    .formatted(userClass.getName(), from, to));
        }
        @SuppressWarnings("unchecked")
        var typed = (Migration<Object, Object>) migration;
        return new Step(oldType, newType, from, to, annotation.fromType(), typed);
    }

    private static Map<String, Chain> buildChains(List<Step> steps) {
        var byNewType = new LinkedHashMap<String, Step>();
        for (var step : steps) {
            if (byNewType.put(step.newType().toString(), step) != null) {
                throw new IllegalStateException("Multiple migrations produce %s".formatted(step.newType()));
            }
        }
        var oldTypes = steps.stream().map(step -> step.oldType().toString()).collect(java.util.stream.Collectors.toSet());
        var chains = new LinkedHashMap<String, Chain>();
        byNewType.keySet().stream().filter(type -> !oldTypes.contains(type))
                .forEach(tip -> chains.put(tip, chainEndingAt(tip, byNewType)));
        return chains;
    }

    private static Chain chainEndingAt(String tip, Map<String, Step> byNewType) {
        var steps = new ArrayList<Step>();
        var visited = new HashSet<String>();
        for (var step = byNewType.get(tip); step != null; step = byNewType.get(step.oldType().toString())) {
            if (!visited.add(step.newType().toString())) {
                throw new IllegalStateException("Migrations of %s form a cycle".formatted(tip));
            }
            steps.addFirst(step);
        }
        for (var i = 1; i < steps.size(); i++) {
            if (steps.get(i - 1).to() != steps.get(i).from()) {
                throw new IllegalStateException(
                        "Migrations of %s are not contiguous: version %d is followed by a migration from version %d"
                                .formatted(tip, steps.get(i - 1).to(), steps.get(i).from()));
            }
        }
        var target = steps.getLast().newType();
        var currentMajor = target.hasGenerics()
                ? EventVersion.parse(EventVersions.declarationFor(EventType.from(target.getType())).version()).major()
                : EventVersions.current(target.resolve(Object.class)).major();
        if (steps.getLast().to() != currentMajor) {
            throw new IllegalStateException("Migrations of %s end at major version %d, but the type declares major version %d"
                    .formatted(tip, steps.getLast().to(), currentMajor));
        }
        return new Chain(steps);
    }

    private ResolvableType tipOf(Step step) {
        return chains.entrySet().stream()
                .filter(entry -> entry.getValue().steps().contains(step))
                .map(entry -> chainTypes.get(entry.getKey()))
                .findFirst()
                .orElseThrow();
    }

    private record Step(
            ResolvableType oldType,
            ResolvableType newType,
            int from,
            int to,
            String fromType,
            Migration<Object, Object> migration
    ) {
    }

    private record Chain(List<Step> steps) {
        int indexOfFrom(int major) {
            for (var i = 0; i < steps.size(); i++) {
                if (steps.get(i).from() == major) {
                    return i;
                }
            }
            return -1;
        }
    }

    /** How a message is read: directly, or after migrating it to the current shape. */
    public final class Plan {
        private final ResolvableType startType;
        private final List<Step> steps;
        private final MessageVersions versions;
        private final boolean componentMigration;

        private Plan(ResolvableType startType, List<Step> steps, MessageVersions versions, boolean componentMigration) {
            this.startType = startType;
            this.steps = steps;
            this.versions = versions;
            this.componentMigration = componentMigration;
        }

        /**
         * Whether the message must be migrated. If not, it can be read directly into the current type.
         *
         * @return {@code true} if {@link #apply} must be used to read the message
         */
        public boolean isRequired() {
            return !steps.isEmpty() || componentMigration;
        }

        /**
         * Migrates the payload: nested components first, then the event, one major version at a time.
         *
         * @param payload the message payload as a tree
         * @param mapper  the mapper that binds the payload to the snapshot types
         * @return the event in its current shape
         */
        public Object apply(JsonNode payload, JsonMapper mapper) {
            var migrated = migrateComponents(payload, startType, versions, mapper);
            return migrate(mapper.readValue(migrated.toString(),
                    mapper.getTypeFactory().constructType(startType.getType())), steps);
        }
    }
}
