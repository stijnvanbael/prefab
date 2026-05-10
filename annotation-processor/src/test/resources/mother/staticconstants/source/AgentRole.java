package mother.staticconstants.source;

public record AgentRole(String value) {

    public static final AgentRole PLANNER = new AgentRole("PLANNER");
    public static final AgentRole RESEARCHER = new AgentRole("RESEARCHER");
    public static final AgentRole REVIEWER = new AgentRole("REVIEWER");

    public static AgentRole of(String value) {
        return new AgentRole(value);
    }
}

