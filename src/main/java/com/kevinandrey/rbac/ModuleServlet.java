package com.kevinandrey.rbac;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Serves the module pages.
 *
 * <p>This servlet does no authorization of its own: by the time a request arrives the container has
 * already applied the matching {@code security-constraint}. Keeping the check out of the code is the
 * point of declarative security.
 */
public final class ModuleServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String context = request.getContextPath();
        String path = request.getServletPath().substring(Module.SECURE_PREFIX.length())
                + (request.getPathInfo() == null ? "" : request.getPathInfo());
        Optional<Module> module = Module.at(path);
        if (module.isEmpty()) {
            Html.page(response, HttpServletResponse.SC_NOT_FOUND, context, "Página no encontrada",
                    "<p>No existe un módulo en esta dirección.</p>" + backLink(context));

            return;
        }
        Module current = module.get();
        String body = "<p>Área: " + Html.escape(current.area()) + ". Rol requerido: <strong>"
                + current.role().name() + "</strong> (" + Html.escape(current.role().label()) + ").</p>\n"
                + "<p>Esta página representa la operación. El contenedor solo la entrega a usuarios con el rol"
                + " requerido.</p>\n" + backLink(context);

        Html.page(response, HttpServletResponse.SC_OK, context, current.title(), body);
    }

    private static String backLink(String context) {
        return "<p><a href=\"" + Html.escape(context) + "/secure/menu\">Volver al menú</a></p>";
    }
}
