package com.kevinandrey.rbac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Checks that the declarative rules in web.xml and the {@link Module} catalog agree. */
class WebXmlTest {

    private record Constraint(String pattern, Set<String> roles) {
    }

    private static Document webXml;
    private static List<Constraint> constraints;

    @BeforeAll
    static void parse() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        webXml = factory.newDocumentBuilder().parse(Path.of("src/main/webapp/WEB-INF/web.xml").toFile());
        constraints = new ArrayList<>();
        NodeList nodes = webXml.getElementsByTagNameNS("*", "security-constraint");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element constraint = (Element) nodes.item(i);
            Set<String> roles = texts(constraint, "role-name");
            texts(constraint, "url-pattern").forEach(pattern -> constraints.add(new Constraint(pattern, roles)));
        }
    }

    private static Set<String> texts(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagNameNS("*", tag);
        Set<String> values = new TreeSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            values.add(nodes.item(i).getTextContent().trim());
        }

        return values;
    }

    /** Servlet specification order: exact match first, then the longest path prefix. */
    private static Optional<Constraint> winning(String url) {
        Optional<Constraint> exact = constraints.stream().filter(c -> c.pattern().equals(url)).findFirst();
        if (exact.isPresent()) {
            return exact;
        }

        return constraints.stream()
                .filter(c -> c.pattern().endsWith("/*"))
                .filter(c -> url.startsWith(c.pattern().substring(0, c.pattern().length() - 1)))
                .max(Comparator.comparingInt(c -> c.pattern().length()));
    }

    @Test
    void everyModuleIsGuardedByExactlyItsRole() {
        for (Module module : Module.values()) {
            Constraint constraint = winning(module.url()).orElseThrow(() -> new AssertionError(module.url()));
            assertEquals(Set.of(module.role().name()), constraint.roles(), module.url());
        }
    }

    @Test
    void theSecureAreaRequiresOneOfTheDeclaredRoles() {
        Set<String> declared = Arrays.stream(Role.values()).map(Enum::name).collect(Collectors.toSet());

        assertEquals(declared, winning("/secure/menu").orElseThrow().roles());
        assertEquals(declared, texts(webXml.getDocumentElement(), "role-name"));
    }

    @Test
    void usesFormLoginAndDeniesUncoveredMethods() {
        assertEquals(Set.of("FORM"), texts(webXml.getDocumentElement(), "auth-method"));
        assertTrue(webXml.getElementsByTagNameNS("*", "deny-uncovered-http-methods").getLength() == 1);
    }

    @Test
    void moduleLookupMatchesPathsOnly() {
        assertEquals(Optional.of(Module.MANAGE_QUOTES), Module.at("/ventas/presupuestos"));
        assertEquals(Optional.empty(), Module.at("/ventas"));
        assertEquals("/secure/compras/autorizar", Module.AUTHORIZE_PURCHASES.url());
        assertEquals("Compras", Module.MANAGE_SUPPLIERS.area());
    }

    @Test
    void htmlEscapingCoversMarkupCharacters() {
        assertEquals("&lt;a href=&quot;x&quot;&gt;O&#39;Brien &amp; co&lt;/a&gt;", Html.escape("<a href=\"x\">O'Brien & co</a>"));
    }
}
