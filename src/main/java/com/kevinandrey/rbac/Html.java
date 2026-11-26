package com.kevinandrey.rbac;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/** Minimal HTML rendering shared by the servlets. */
final class Html {

    private Html() {
    }

    /**
     * Escapes text for element content and quoted attributes.
     *
     * @param text untrusted text
     * @return escaped text
     */
    static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }

        return out.toString();
    }

    /**
     * Writes a complete page.
     *
     * @param response response to fill
     * @param status HTTP status
     * @param contextPath application context path, used for the stylesheet
     * @param title page title, escaped here
     * @param body trusted HTML for the main element
     * @throws IOException when the response cannot be written
     */
    static void page(HttpServletResponse response, int status, String contextPath, String title, String body)
            throws IOException {
        response.setStatus(status);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<!doctype html>");
        out.println("<html lang=\"es\">");
        out.println("<head>");
        out.println("<meta charset=\"utf-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("<title>" + escape(title) + " | Sistema de gestión</title>");
        out.println("<link rel=\"stylesheet\" href=\"" + escape(contextPath) + "/estilos.css\">");
        out.println("</head>");
        out.println("<body>");
        out.println("<main>");
        out.println("<h1>" + escape(title) + "</h1>");
        out.println(body);
        out.println("</main>");
        out.println("</body>");
        out.println("</html>");
    }
}
