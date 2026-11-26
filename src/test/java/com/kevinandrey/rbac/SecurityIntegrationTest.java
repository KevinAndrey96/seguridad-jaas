package com.kevinandrey.rbac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.realm.SecretKeyCredentialHandler;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Boots the real web application on embedded Tomcat and drives it over HTTP like a browser. */
class SecurityIntegrationTest {

    private static final String PASSWORD = "clave-de-prueba";

    private static final Map<String, Set<Role>> USERS = Map.of(
            "autoriza", EnumSet.of(Role.AC),
            "compras", EnumSet.of(Role.GC),
            "nominas", EnumSet.of(Role.GN),
            "ventas", EnumSet.of(Role.GV),
            "gerencia", EnumSet.of(Role.GC, Role.GN),
            "bloqueo", EnumSet.of(Role.GV));

    @TempDir
    static Path workDir;

    private static Tomcat tomcat;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        Path users = workDir.resolve("users.xml");
        Files.writeString(users, usersFile(), StandardCharsets.UTF_8);
        System.setProperty("users.file", users.toString());

        tomcat = new Tomcat();
        tomcat.setBaseDir(workDir.resolve("tomcat").toString());
        tomcat.setPort(0);
        tomcat.getConnector();
        tomcat.addWebapp("", Path.of("src/main/webapp").toAbsolutePath().toString());
        tomcat.start();
        base = "http://localhost:" + tomcat.getConnector().getLocalPort();
    }

    @AfterAll
    static void stop() throws LifecycleException {
        tomcat.stop();
        tomcat.destroy();
        System.clearProperty("users.file");
    }

    private static String usersFile() throws Exception {
        SecretKeyCredentialHandler handler = new SecretKeyCredentialHandler();
        handler.setAlgorithm("PBKDF2WithHmacSHA512");
        handler.setIterations(210_000);
        handler.setKeyLength(256);
        handler.setSaltLength(16);
        String hash = handler.mutate(PASSWORD);
        String entries = USERS.entrySet().stream()
                .map(e -> "  <user username=\"" + e.getKey() + "\" password=\"" + hash + "\" roles=\""
                        + e.getValue().stream().map(Enum::name).collect(Collectors.joining(",")) + "\"/>")
                .collect(Collectors.joining("\n"));

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tomcat-users>\n" + entries + "\n</tomcat-users>\n";
    }

    /** A cookie keeping HTTP client, one per simulated browser. */
    private static final class Browser {

        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpResponse<String> get(String path) throws IOException, InterruptedException {
            return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> post(String path, Map<String, String> form) throws IOException, InterruptedException {
            String body = form.entrySet().stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> login(String user, String password) throws IOException, InterruptedException {
            get("/secure/menu");

            return post("/secure/j_security_check", Map.of("j_username", user, "j_password", password));
        }
    }

    private static Browser signedIn(String user) throws IOException, InterruptedException {
        Browser browser = new Browser();
        HttpResponse<String> menu = browser.login(user, PASSWORD);
        assertEquals(200, menu.statusCode());
        assertTrue(menu.body().contains("Menú principal"), () -> user + " did not reach the menu");

        return browser;
    }

    @Test
    void anonymousVisitorsGetTheLoginForm() throws Exception {
        HttpResponse<String> response = new Browser().get("/secure/ventas/clientes");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("action=\"j_security_check\""));
        assertFalse(response.body().contains("Gestionar clientes"));
    }

    @Test
    void everyModuleIsOpenOnlyToItsRole() throws Exception {
        for (Map.Entry<String, Set<Role>> user : USERS.entrySet()) {
            if (user.getKey().equals("bloqueo")) {
                continue;
            }
            Browser browser = signedIn(user.getKey());
            for (Module module : Module.values()) {
                int expected = user.getValue().contains(module.role()) ? 200 : 403;
                HttpResponse<String> response = browser.get(module.url());
                assertEquals(expected, response.statusCode(), user.getKey() + " on " + module.url());
                String marker = expected == 200 ? "<h1>" + module.title() + "</h1>" : "<h1>Acceso denegado</h1>";
                assertTrue(response.body().contains(marker), user.getKey() + " on " + module.url());
            }
        }
    }

    @Test
    void theMenuMarksWhatEachUserMayOpen() throws Exception {
        String menu = signedIn("gerencia").get("/secure/menu").body();

        assertTrue(menu.contains("Sesión iniciada como <strong>gerencia</strong>"));
        assertTrue(menu.contains("GC (Gestión de compras), GN (Gestión de nóminas)"));
        assertTrue(menu.contains("<li class=\"permitido\"><a href=\"/secure/compras/proveedores\">"));
        assertTrue(menu.contains("<li class=\"permitido\"><a href=\"/secure/nominas/trabajadores\">"));
        assertTrue(menu.contains("<li class=\"denegado\"><a href=\"/secure/compras/autorizar\">"));
        assertTrue(menu.contains("<li class=\"denegado\"><a href=\"/secure/ventas/facturas\">"));
    }

    @Test
    void unknownModulePathsAreNotFoundForTheAreaRole() throws Exception {
        assertEquals(404, signedIn("ventas").get("/secure/ventas/otra").statusCode());
        assertEquals(403, signedIn("compras").get("/secure/ventas/otra").statusCode());
        assertEquals(404, signedIn("compras").get("/secure/compras/otra").statusCode());
    }

    @Test
    void wrongPasswordsShowTheErrorPage() throws Exception {
        HttpResponse<String> response = new Browser().login("ventas", "incorrecta");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Usuario o contraseña incorrectos"));
    }

    @Test
    void fiveFailuresLockTheAccount() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            new Browser().login("bloqueo", "incorrecta");
        }

        HttpResponse<String> response = new Browser().login("bloqueo", PASSWORD);
        assertTrue(response.body().contains("Usuario o contraseña incorrectos"));
    }

    @Test
    void aLoginPostedWithoutAPriorRequestFallsBackToTheLandingPage() throws Exception {
        Browser browser = new Browser();
        Map<String, String> credentials = Map.of("j_username", "nominas", "j_password", PASSWORD);

        HttpResponse<String> landing = browser.post("/j_security_check", credentials);
        assertEquals(200, landing.statusCode());
        assertEquals("/secure/menu", landing.uri().getPath());
        assertTrue(landing.body().contains("j_security_check"));

        HttpResponse<String> menu = browser.post("/secure/j_security_check", credentials);
        assertTrue(menu.body().contains("Menú principal"));
    }

    @Test
    void logoutEndsTheSessionAndOnlyAcceptsPost() throws Exception {
        Browser browser = signedIn("ventas");

        assertEquals(405, browser.get("/secure/salir").statusCode());
        HttpResponse<String> home = browser.post("/secure/salir", Map.of());
        assertEquals(200, home.statusCode());
        assertTrue(home.body().contains("Entrar al sistema"));
        assertTrue(browser.get("/secure/menu").body().contains("j_security_check"));
    }

    @Test
    void responsesCarryHardeningHeadersAndStaticFilesArePublic() throws Exception {
        HttpResponse<String> css = new Browser().get("/estilos.css");
        assertEquals(200, css.statusCode());
        assertTrue(css.body().contains("li.permitido"));

        HttpResponse<String> menu = signedIn("autoriza").get("/secure/menu");
        assertEquals("nosniff", menu.headers().firstValue("X-Content-Type-Options").orElseThrow());
        assertEquals("DENY", menu.headers().firstValue("X-Frame-Options").orElseThrow());
        assertEquals("no-store", menu.headers().firstValue("Cache-Control").orElseThrow());
        assertTrue(menu.headers().firstValue("Content-Security-Policy").orElseThrow().startsWith("default-src 'self'"));
    }
}
