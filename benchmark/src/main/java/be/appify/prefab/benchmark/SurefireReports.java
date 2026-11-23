package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/** Reads Surefire XML reports ({@code target/surefire-reports/TEST-*.xml}). */
final class SurefireReports {
    private static final int MAX_MESSAGE_LENGTH = 1500;

    private SurefireReports() {
    }

    /**
     * Summarises the reports of the test classes that match a filter.
     *
     * @param workspace   project root
     * @param testClasses filter on fully qualified test class names
     * @return the summary; {@code reportsFound} is 0 when the tests never ran (for example a compile error)
     */
    static TestSummary read(Path workspace, Predicate<String> testClasses) {
        var reports = FileTrees.files(workspace.resolve("target/surefire-reports")).stream()
                .filter(file -> file.getFileName().toString().startsWith("TEST-"))
                .filter(file -> file.getFileName().toString().endsWith(".xml"))
                .filter(file -> testClasses.test(className(file)))
                .toList();
        int tests = 0;
        int failed = 0;
        int skipped = 0;
        var failures = new ArrayList<String>();
        for (var report : reports) {
            var suite = parse(report);
            tests += intAttribute(suite, "tests");
            failed += intAttribute(suite, "failures") + intAttribute(suite, "errors");
            skipped += intAttribute(suite, "skipped");
            var testCases = suite.getElementsByTagName("testcase");
            for (int i = 0; i < testCases.getLength(); i++) {
                var testCase = (Element) testCases.item(i);
                problem(testCase).ifPresent(problem -> failures.add(
                        testCase.getAttribute("classname") + "." + testCase.getAttribute("name") + ": " + problem));
            }
        }
        return new TestSummary(reports.size(), tests, failed, skipped, List.copyOf(failures));
    }

    private static java.util.Optional<String> problem(Element testCase) {
        for (var tag : List.of("failure", "error")) {
            var nodes = testCase.getElementsByTagName(tag);
            if (nodes.getLength() > 0) {
                var element = (Element) nodes.item(0);
                var message = element.getAttribute("message");
                var text = message.isBlank() ? element.getTextContent() : message;
                return java.util.Optional.of(truncate(text.strip()));
            }
        }
        return java.util.Optional.empty();
    }

    private static String truncate(String text) {
        return text.length() <= MAX_MESSAGE_LENGTH ? text : text.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }

    private static String className(Path report) {
        var name = report.getFileName().toString();
        return name.substring("TEST-".length(), name.length() - ".xml".length());
    }

    private static Element parse(Path report) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(report.toFile()).getDocumentElement();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse " + report, e);
        }
    }

    private static int intAttribute(Element element, String name) {
        var value = element.getAttribute(name);
        return value.isBlank() ? 0 : (int) Double.parseDouble(value);
    }

    static void clear(Path workspace) {
        FileTrees.delete(workspace.resolve("target/surefire-reports"));
    }

    static boolean exists(Path workspace) {
        return Files.isDirectory(workspace.resolve("target/surefire-reports"));
    }

    /**
     * Test results.
     *
     * @param reportsFound number of report files (test classes that ran)
     * @param tests        tests run
     * @param failed       failures plus errors
     * @param skipped      skipped tests
     * @param failures     one line per failed test with its message
     */
    record TestSummary(int reportsFound, int tests, int failed, int skipped, List<String> failures) {
        boolean allPassed() {
            return reportsFound > 0 && tests > 0 && failed == 0;
        }
    }
}
