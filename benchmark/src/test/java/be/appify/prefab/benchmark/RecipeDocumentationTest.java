package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeDocumentationTest {
    private static final int MAX_RECIPE_CHARACTERS = 3_000;
    private static final Pattern JAVA_LINK = Pattern.compile("\\]\\(([^)]+\\.java)\\)");
    private static final BenchmarkPaths PATHS = BenchmarkPaths.discover(Path.of("."));
    private static final List<Path> VERIFIED_SOURCE_ROOTS = List.of(
            PATHS.repository().resolve("examples/showcase/src/main/java"),
            PATHS.repository().resolve("examples/showcase/src/test/java"),
            PATHS.repository().resolve("annotation-processor/src/test/java"),
            PATHS.repository().resolve("annotation-processor/src/test/resources/tenant/project/source"));

    @Test
    void everyRecipeStaysWithinBudgetAndLinksToBuildVerifiedJavaSources() throws Exception {
        var recipes = PATHS.repository().resolve("backlog/docs/recipes");
        var index = Files.readString(recipes.resolve("index.md"));
        try (var files = Files.list(recipes)) {
            var recipeFiles = files.filter(path -> path.getFileName().toString().endsWith(".md"))
                    .filter(path -> !path.getFileName().toString().equals("index.md"))
                    .toList();

            assertThat(recipeFiles).hasSizeGreaterThanOrEqualTo(13);
            for (var recipe : recipeFiles) {
                var content = Files.readString(recipe);
                assertThat(content.length()).as(recipe.getFileName().toString())
                        .isLessThanOrEqualTo(MAX_RECIPE_CHARACTERS);
                assertThat(content).as(recipe.getFileName().toString()).contains("**Budget:** 750 tokens.");
                assertThat(content).as(recipe.getFileName().toString()).doesNotContain("```java");
                assertThat(index).as(recipe.getFileName().toString())
                        .contains("(" + recipe.getFileName() + ")");

                var matcher = JAVA_LINK.matcher(content);
                var exampleCount = 0;
                while (matcher.find()) {
                    var example = recipe.getParent().resolve(matcher.group(1)).normalize();
                    assertThat(example).as(recipe.getFileName().toString()).startsWith(PATHS.repository());
                    assertThat(example).as(recipe.getFileName().toString()).exists();
                    assertThat(VERIFIED_SOURCE_ROOTS.stream().anyMatch(example::startsWith))
                            .as(recipe.getFileName().toString())
                            .isTrue();
                    exampleCount++;
                }
                assertThat(exampleCount).as(recipe.getFileName().toString())
                        .as("link to a complete Java example compiled or exercised by the Maven build")
                        .isPositive();
            }
        }
    }
}
