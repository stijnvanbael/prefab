package event.handler.createorupdate;

import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record MessageSent(
        Reference<ChannelSummary> summary,
        String text
) {
}
