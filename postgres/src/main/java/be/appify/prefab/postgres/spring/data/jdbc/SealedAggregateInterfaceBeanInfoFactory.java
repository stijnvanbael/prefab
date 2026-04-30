package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.annotations.Aggregate;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeanInfoFactory;

/**
 * A {@link BeanInfoFactory} that exposes the accessor methods of sealed interfaces annotated with
 * {@link Aggregate} as readable JavaBean properties.
 *
 * <p>Spring Data's {@code TypeDiscoverer} resolves property types via {@code BeanUtils.getPropertyDescriptor},
 * which only recognises JavaBean-style {@code getXxx()} methods. Sealed aggregate interfaces use record-style
 * accessors (e.g. {@code quiz()} instead of {@code getQuiz()}), so Spring Data cannot find those properties and
 * throws a {@code PropertyReferenceException} when a repository query method (or a {@code @RepositoryMixin}
 * method) references such a property.</p>
 *
 * <p>This factory is registered via {@code META-INF/spring.factories} and is picked up by
 * {@code CachedIntrospectionResults} for every sealed {@link Aggregate} interface, making all its no-arg
 * methods visible to Spring Data's property-path resolution machinery.</p>
 */
public class SealedAggregateInterfaceBeanInfoFactory implements BeanInfoFactory {

    private static final Set<String> EXCLUDED_METHODS = Set.of(
            "getClass", "hashCode", "toString", "wait", "notify", "notifyAll");

    @Override
    public @Nullable BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
        if (!beanClass.isInterface() || !beanClass.isSealed() || !beanClass.isAnnotationPresent(Aggregate.class)) {
            return null;
        }
        return new SealedInterfaceBeanInfo(beanClass);
    }

    private static class SealedInterfaceBeanInfo extends SimpleBeanInfo {

        private final PropertyDescriptor[] propertyDescriptors;

        SealedInterfaceBeanInfo(Class<?> type) throws IntrospectionException {
            this.propertyDescriptors = buildPropertyDescriptors(type);
        }

        private static PropertyDescriptor[] buildPropertyDescriptors(Class<?> type) throws IntrospectionException {
            List<PropertyDescriptor> descriptors = new ArrayList<>();
            for (Method method : type.getMethods()) {
                if (isPropertyAccessor(method)) {
                    descriptors.add(new PropertyDescriptor(method.getName(), method, null));
                }
            }
            return descriptors.toArray(PropertyDescriptor[]::new);
        }

        private static boolean isPropertyAccessor(Method method) {
            return method.getParameterCount() == 0
                    && method.getReturnType() != void.class
                    && !EXCLUDED_METHODS.contains(method.getName())
                    && !method.getName().startsWith("get")
                    && !method.getName().startsWith("is");
        }

        @Override
        public PropertyDescriptor[] getPropertyDescriptors() {
            return propertyDescriptors;
        }
    }
}

