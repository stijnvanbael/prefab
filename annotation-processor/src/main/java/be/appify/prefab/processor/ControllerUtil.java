package be.appify.prefab.processor;

import org.springframework.data.domain.Sort;
import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static org.apache.commons.text.WordUtils.uncapitalize;
import static org.atteo.evo.inflector.English.plural;

import java.util.ArrayList;

public class ControllerUtil {
    public static String pathOf(ClassManifest manifest) {
        var parentPath = manifest.parent()
                .map(parent -> "%s/{%sId}/".formatted(
                        toKebabCase(plural(parent.type().parameters().getFirst().simpleName())),
                        uncapitalize(parent.type().parameters().getFirst().simpleName())))
                .orElse("");
        return parentPath + toKebabCase(plural(manifest.simpleName()));
    }

    public static String[] toRequestParams(Sort sort) {
        var params = new ArrayList<String>();
        sort.stream().forEach(order ->
                params.add(order.getProperty() + "," + order.getDirection().name()));
        return params.toArray(new String[0]);
    }
}
