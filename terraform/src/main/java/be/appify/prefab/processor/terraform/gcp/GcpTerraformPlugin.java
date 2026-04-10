package be.appify.prefab.processor.terraform.gcp;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import java.util.List;

@SuppressWarnings("unused")
public class GcpTerraformPlugin implements PrefabPlugin {
    private PrefabContext context;

    public GcpTerraformPlugin() {}

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, List<PolymorphicAggregateManifest> polymorphicManifests) {
        new GcpTerraformWriter(context).write(manifests);
    }
}
