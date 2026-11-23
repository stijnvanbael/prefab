package be.appify.prefab.benchmark;

import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/**
 * Versions the generated workspaces build against, read from the repository's root {@code pom.xml} so the benchmark
 * always measures the Prefab version in the working tree (installed in the local Maven repository).
 *
 * @param prefab     Prefab version
 * @param springBoot Spring Boot parent version
 * @param java       Java release
 */
record ProjectVersions(String prefab, String springBoot, String java) {

    static ProjectVersions read(Path repository) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var document = factory.newDocumentBuilder().parse(repository.resolve("pom.xml").toFile());
            var project = document.getDocumentElement();
            var parent = (Element) project.getElementsByTagName("parent").item(0);
            var properties = (Element) project.getElementsByTagName("properties").item(0);
            return new ProjectVersions(
                    directChild(project, "version"),
                    directChild(parent, "version"),
                    directChild(properties, "maven.compiler.release"));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read versions from " + repository.resolve("pom.xml"), e);
        }
    }

    private static String directChild(Element element, String name) {
        var children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && child.getTagName().equals(name)) {
                return child.getTextContent().trim();
            }
        }
        throw new IllegalStateException("<" + element.getTagName() + "> has no <" + name + ">");
    }
}
