package com.kevinandrey.rbac;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lists every module and marks the ones the signed in user may open, as the 2017 menu did.
 *
 * <p>The marks come from {@link HttpServletRequest#isUserInRole(String)}; the container still enforces
 * the constraints in {@code web.xml} if a user follows a link marked as unavailable.
 */
public final class MenuServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String context = request.getContextPath();
        String user = request.getUserPrincipal().getName();
        String roles = Stream.of(Role.values())
                .filter(role -> request.isUserInRole(role.name()))
                .map(role -> role.name() + " (" + role.label() + ")")
                .collect(Collectors.joining(", "));

        Map<String, List<Module>> byArea = new LinkedHashMap<>();
        for (Module module : Module.values()) {
            byArea.computeIfAbsent(module.area(), area -> new ArrayList<>()).add(module);
        }

        StringBuilder body = new StringBuilder();
        body.append("<p>Sesión iniciada como <strong>").append(Html.escape(user)).append("</strong>. Roles: ")
                .append(Html.escape(roles.isEmpty() ? "ninguno" : roles)).append(".</p>\n");
        byArea.forEach((area, modules) -> {
            body.append("<h2>").append(Html.escape(area)).append("</h2>\n<ul>\n");
            for (Module module : modules) {
                boolean allowed = request.isUserInRole(module.role().name());
                body.append("<li class=\"").append(allowed ? "permitido" : "denegado").append("\">")
                        .append("<a href=\"").append(Html.escape(context + module.url())).append("\">")
                        .append(Html.escape(module.title())).append("</a> ")
                        .append(allowed ? "permitido" : "sin acceso")
                        .append(" <small>(rol ").append(module.role().name()).append(")</small></li>\n");
            }
            body.append("</ul>\n");
        });
        body.append("<form method=\"post\" action=\"").append(Html.escape(context)).append("/secure/salir\">")
                .append("<button type=\"submit\">Cerrar sesión</button></form>");

        Html.page(response, HttpServletResponse.SC_OK, context, "Menú principal", body.toString());
    }
}
