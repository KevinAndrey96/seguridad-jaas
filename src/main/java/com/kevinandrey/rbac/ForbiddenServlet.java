package com.kevinandrey.rbac;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Renders the {@code 403} error page configured in {@code web.xml}. */
public final class ForbiddenServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String context = request.getContextPath();
        Html.page(response, HttpServletResponse.SC_FORBIDDEN, context, "Acceso denegado",
                "<p>Tu usuario no tiene el rol que exige esta operación.</p>"
                        + "<p><a href=\"" + Html.escape(context) + "/secure/menu\">Volver al menú</a></p>");
    }
}
