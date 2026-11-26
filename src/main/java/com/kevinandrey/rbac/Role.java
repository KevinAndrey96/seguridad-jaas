package com.kevinandrey.rbac;

/**
 * Security roles declared in {@code web.xml}.
 *
 * <p>The constant names are the role names the container checks, so they must match the
 * {@code <role-name>} entries and the {@code roles} attribute of each user in the users file.
 */
public enum Role {
    /** Authorizes purchases. */
    AC("Autorización de compras"),
    /** Manages purchases and suppliers. */
    GC("Gestión de compras"),
    /** Manages payroll and workers. */
    GN("Gestión de nóminas"),
    /** Manages customers, invoices and quotes. */
    GV("Gestión de ventas");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /**
     * Returns the Spanish description shown in the menu.
     *
     * @return role label
     */
    public String label() {
        return label;
    }
}
