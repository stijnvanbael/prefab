package mother.staticconstants.source;

import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;

@Generate(plugin = AssertionPlugin.class, enabled = false)
public record AgentRole(String value) {

    public static final AgentRole PLANNER = new AgentRole("PLANNER");
    public static final AgentRole RESEARCHER = new AgentRole("RESEARCHER");
    public static final AgentRole REVIEWER = new AgentRole("REVIEWER");

    public static AgentRole of(String value) {
        return new AgentRole(value);
    }
}

