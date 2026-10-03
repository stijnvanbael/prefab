package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.rest.create.CreatePlugin;
import be.appify.prefab.processor.rest.delete.DeletePlugin;
import be.appify.prefab.processor.rest.getbyid.GetByIdPlugin;
import be.appify.prefab.processor.rest.getlist.GetListPlugin;
import be.appify.prefab.processor.rest.update.UpdatePlugin;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestOperationPluginStructureTest {

    @Test
    void restOperationPluginsShareANonPublicLifecycleBaseClass() {
        var baseClass = RestOperationPluginSupport.class;
        var bridgeClass = RestOperationPlugin.class;

        assertFalse(Modifier.isPublic(baseClass.getModifiers()));
        assertTrue(Modifier.isAbstract(baseClass.getModifiers()));
        assertEquals(baseClass, bridgeClass.getSuperclass());
        List.of(
                        CreatePlugin.class,
                        UpdatePlugin.class,
                        DeletePlugin.class,
                        GetByIdPlugin.class,
                        GetListPlugin.class)
                .forEach(pluginClass -> assertEquals(bridgeClass, pluginClass.getSuperclass(),
                        () -> pluginClass.getSimpleName() + " should extend the shared REST base class"));
    }
}




