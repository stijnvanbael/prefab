package be.appify.prefab.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static checks on the files an agent added or changed. They catch the typical ways a model falls back on plain
 * Spring habits instead of letting Prefab generate the code: hand-written controllers, JPA entities, Spring Data
 * repositories, hand-written persistence, Lombok, overriding generated classes or hand-written migrations.
 *
 * <p>A task can allow a rule with {@code allowGuardrails} in its {@code task.yml}, for example when the task is about a
 * documented extension point that legitimately needs such code.
 */
final class Guardrails {

    static final List<Rule> RULES = List.of(
            new Rule("hand-written-controller", Severity.FAIL,
                    "hand-written Spring MVC controller; Prefab generates controllers from @Create/@Update/@GetList/...",
                    Pattern.compile("@(Rest)?Controller\\b|@(Request|Get|Post|Put|Patch|Delete)Mapping\\b")),
            new Rule("jpa", Severity.FAIL,
                    "JPA mapping; Prefab persists aggregates with Spring Data JDBC or MongoDB",
                    Pattern.compile("import\\s+jakarta\\.persistence\\.|@Entity\\b|@Table\\(")),
            new Rule("spring-data-repository", Severity.FAIL,
                    "hand-written Spring Data repository; use the generated repository and @RepositoryMixin",
                    Pattern.compile("extends\\s+(Jpa|Crud|ListCrud|PagingAndSorting|ListPagingAndSorting|Mongo)?Repository<")),
            new Rule("hand-written-persistence", Severity.FAIL,
                    "hand-written database access; use generated repositories and @RepositoryMixin queries",
                    Pattern.compile("\\b(Named(Parameter)?)?JdbcTemplate\\b|\\bJdbcClient\\b|\\bMongoTemplate\\b|\\bEntityManager\\b")),
            new Rule("hand-written-consumer", Severity.FAIL,
                    "hand-written message listener; use @EventHandler on the aggregate and let Prefab generate the consumer",
                    Pattern.compile("@(KafkaListener|SqsListener)\\b|implements\\s+MessageReceiver\\b")),
            new Rule("lombok", Severity.FAIL,
                    "Lombok; Prefab aggregates are Java records",
                    Pattern.compile("import\\s+lombok\\.")),
            new Rule("generated-class-override", Severity.FAIL,
                    "source file that replaces a class Prefab generates (controller, service, repository, request or response)",
                    null),
            new Rule("hand-written-migration", Severity.WARN,
                    "new SQL migration under src/main/resources/db/migration; Prefab generates migrations at compile time",
                    null));

    static final Set<String> RULE_IDS = RULES.stream().map(Rule::id).collect(Collectors.toUnmodifiableSet());

    /** Rules that mean the agent replaced generated code, or hand-wrote an alternative to it. */
    static final Set<String> OVERRIDE_RULES = Set.of("generated-class-override", "hand-written-controller", "jpa",
            "spring-data-repository", "hand-written-persistence", "hand-written-consumer");

    private static final Pattern AGGREGATE = Pattern.compile("@Aggregate\\b[\\s\\S]*?\\b(record|class)\\s+(\\w+)");
    private static final String MIGRATION_DIRECTORY = "src/main/resources/db/migration/";

    private Guardrails() {
    }

    /**
     * Checks the changed files of a workspace.
     *
     * @param workspace the project root
     * @param changes   files changed since the starting snapshot
     * @param allowed   rule ids the task allows
     * @return violations, in file order
     */
    static List<Violation> check(Path workspace, List<Git.ChangedFile> changes, Set<String> allowed) {
        var aggregates = aggregateNames(workspace);
        var violations = new ArrayList<Violation>();
        for (var change : changes) {
            if (change.status() == 'D') {
                continue;
            }
            var path = change.path();
            if (path.startsWith(MIGRATION_DIRECTORY) && path.endsWith(".sql") && change.status() == 'A') {
                violations.add(violation("hand-written-migration", path, 0));
            }
            if (path.startsWith("src/main/java/") && path.endsWith(".java")) {
                violations.addAll(checkJavaFile(workspace, path, aggregates));
            }
        }
        return violations.stream().filter(violation -> !allowed.contains(violation.rule())).toList();
    }

    private static List<Violation> checkJavaFile(Path workspace, String path, Set<String> aggregates) {
        var violations = new ArrayList<Violation>();
        var simpleName = Path.of(path).getFileName().toString().replace(".java", "");
        if (overridesGeneratedClass(simpleName, aggregates)) {
            violations.add(violation("generated-class-override", path, 0));
        }
        var lines = readLines(workspace.resolve(path));
        for (var rule : RULES) {
            if (rule.pattern() == null) {
                continue;
            }
            for (int i = 0; i < lines.size(); i++) {
                if (rule.pattern().matcher(lines.get(i)).find()) {
                    violations.add(new Violation(rule.id(), rule.severity(), rule.description(), path, i + 1));
                    break;
                }
            }
        }
        return violations;
    }

    private static boolean overridesGeneratedClass(String simpleName, Set<String> aggregates) {
        return aggregates.stream().anyMatch(aggregate -> Set.of(
                aggregate + "Controller",
                aggregate + "Service",
                aggregate + "Repository",
                aggregate + "Response",
                "Create" + aggregate + "Request").contains(simpleName));
    }

    static Set<String> aggregateNames(Path workspace) {
        var sources = workspace.resolve("src/main/java");
        if (!Files.isDirectory(sources)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.walk(sources)) {
            return files.filter(file -> file.toString().endsWith(".java"))
                    .flatMap(file -> AGGREGATE.matcher(read(file)).results().map(match -> match.group(2)))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Violation violation(String ruleId, String path, int line) {
        var rule = RULES.stream().filter(candidate -> candidate.id().equals(ruleId)).findFirst().orElseThrow();
        return new Violation(rule.id(), rule.severity(), rule.description(), path, line);
    }

    private static List<String> readLines(Path file) {
        return read(file).lines().toList();
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Whether a violation fails the task or is only reported. */
    enum Severity { FAIL, WARN }

    record Rule(String id, Severity severity, String description, Pattern pattern) {
    }

    /**
     * A guardrail violation.
     *
     * @param rule        rule id
     * @param severity    whether it fails the task
     * @param description what is wrong and what to do instead
     * @param file        workspace-relative file
     * @param line        1-based line, or 0 when the whole file is the problem
     */
    record Violation(String rule, Severity severity, String description, String file, int line) {
    }
}
