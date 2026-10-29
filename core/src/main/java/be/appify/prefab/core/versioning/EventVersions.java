package be.appify.prefab.core.versioning;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Versioned;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.core.ResolvableType;

/** Reads the declared versions of event and component types and creates the headers producers stamp on messages. */
public final class EventVersions {
    /** Header carrying the full version of the event contract. */
    public static final String EVENT_VERSION_HEADER = "prefab-event-version";
    /** Header carrying the versions of the versioned components nested in the event, as {@code type=version,...}. */
    public static final String COMPONENT_VERSIONS_HEADER = "prefab-component-versions";

    private static final Map<Class<?>, Set<Class<?>>> VERSIONED_COMPONENTS = new ConcurrentHashMap<>();

    private EventVersions() {
    }

    /**
     * Returns the version declared on the type, or on the nearest annotated supertype.
     *
     * @param type the event or component type
     * @return the declared version, empty if neither {@link Event} nor {@link Versioned} is present
     */
    public static Optional<EventVersion> declared(Class<?> type) {
        return declaredEvent(type).map(Event::version).or(() -> versioned(type).map(Versioned::version))
                .map(EventVersion::parse);
    }

    /**
     * Returns the current version of the type, {@link EventVersion#INITIAL} if no version is declared.
     *
     * @param type the event or component type
     * @return the current version
     */
    public static EventVersion current(Class<?> type) {
        return declared(type).orElse(EventVersion.INITIAL);
    }

    /**
     * Returns the oldest supported version declared on the event type.
     *
     * @param type the event type
     * @return the minimum supported version, empty if not restricted
     */
    public static Optional<EventVersion> minSupported(Class<?> type) {
        return declaredEvent(type).map(Event::minSupportedVersion).filter(value -> !value.isBlank())
                .map(EventVersion::parse);
    }

    /**
     * Creates the headers a producer stamps on a message carrying the given event.
     *
     * @param event the event to publish
     * @return the version headers, empty if the event type does not declare {@link Event}
     */
    public static Map<String, String> headersFor(Object event) {
        var type = event.getClass();
        if (declaredEvent(type).isEmpty()) {
            return Map.of();
        }
        var headers = new LinkedHashMap<String, String>();
        headers.put(EVENT_VERSION_HEADER, current(type).toString());
        var components = VERSIONED_COMPONENTS.computeIfAbsent(type, EventVersions::findVersionedComponents).stream()
                .map(component -> component.getName() + "=" + current(component))
                .collect(Collectors.joining(","));
        if (!components.isEmpty()) {
            headers.put(COMPONENT_VERSIONS_HEADER, components);
        }
        return headers;
    }

    private static Optional<Event> declaredEvent(Class<?> type) {
        return supertypes(type).map(candidate -> candidate.getAnnotation(Event.class)).filter(java.util.Objects::nonNull)
                .findFirst();
    }

    private static Optional<Versioned> versioned(Class<?> type) {
        return supertypes(type).map(candidate -> candidate.getAnnotation(Versioned.class))
                .filter(java.util.Objects::nonNull).findFirst();
    }

    private static java.util.stream.Stream<Class<?>> supertypes(Class<?> type) {
        var supertypes = new LinkedHashSet<Class<?>>();
        collectSupertypes(type, supertypes);
        return supertypes.stream();
    }

    private static void collectSupertypes(Class<?> type, Set<Class<?>> collected) {
        if (type == null || type == Object.class || !collected.add(type)) {
            return;
        }
        collectSupertypes(type.getSuperclass(), collected);
        for (var anInterface : type.getInterfaces()) {
            collectSupertypes(anInterface, collected);
        }
    }

    private static Set<Class<?>> findVersionedComponents(Class<?> type) {
        var found = new LinkedHashSet<Class<?>>();
        collectVersionedComponents(ResolvableType.forClass(type), found, new LinkedHashSet<>());
        return found;
    }

    private static void collectVersionedComponents(ResolvableType type, Set<Class<?>> found, Set<Class<?>> visited) {
        var element = TypeStructure.elementType(type);
        if (element.isPresent()) {
            collectVersionedComponents(element.get(), found, visited);
            return;
        }
        var raw = type.resolve(Object.class);
        if (!raw.isRecord() || Modifier.isAbstract(raw.getModifiers()) || !visited.add(raw)) {
            return;
        }
        if (versioned(raw).isPresent()) {
            found.add(raw);
        }
        for (var component : raw.getRecordComponents()) {
            collectVersionedComponents(ResolvableType.forType(component.getGenericType()), found, visited);
        }
    }
}
