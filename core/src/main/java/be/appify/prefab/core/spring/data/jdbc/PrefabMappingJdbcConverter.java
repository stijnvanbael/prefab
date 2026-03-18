package be.appify.prefab.core.spring.data.jdbc;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.convert.JdbcTypeFactory;
import org.springframework.data.jdbc.core.convert.MappingJdbcConverter;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;

/**
 * Custom MappingJdbcConverter that applies registered custom converters to individual elements when reading array/collection columns.
 * <p>
 * Spring Data JDBC reads SQL arrays (e.g. VARCHAR[]) into Object[] using the JDBC driver, then assembles the list. For custom simple types
 * like {@link be.appify.prefab.core.service.Reference}, the per-element {@code @ReadingConverter} is not applied automatically. This
 * subclass post-processes the list to convert each element via the ConversionService.
 */
public class PrefabMappingJdbcConverter extends MappingJdbcConverter {

    public PrefabMappingJdbcConverter(
            JdbcMappingContext context,
            RelationResolver relationResolver,
            JdbcCustomConversions conversions,
            JdbcTypeFactory typeFactory
    ) {
        super(context, relationResolver, conversions, typeFactory);
    }

    @Override
    @Nullable
    public Object readValue(@Nullable Object value, TypeInformation<?> type) {
        Object result = super.readValue(value, type);

        if (result instanceof List<?> list && type.isCollectionLike() && !list.isEmpty()) {
            TypeInformation<?> componentType = type.getRequiredComponentType();
            Class<?> targetType = componentType.getType();
            Object firstElement = list.getFirst();

            if (firstElement != null
                    && !targetType.isInstance(firstElement)
                    && getConversions().hasCustomReadTarget(firstElement.getClass(), targetType)) {
                List<Object> converted = new ArrayList<>(list.size());
                for (Object element : list) {
                    converted.add(getConversionService().convert(element, targetType));
                }
                return converted;
            }
        }

        return result;
    }
}