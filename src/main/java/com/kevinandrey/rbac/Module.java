package com.kevinandrey.rbac;

import java.util.Arrays;
import java.util.Optional;

/**
 * Business operations behind {@code /secure}, each guarded by exactly one role in {@code web.xml}.
 */
public enum Module {
    /** Approve pending purchase orders. */
    AUTHORIZE_PURCHASES("/compras/autorizar", "Compras", "Autorizar compras", Role.AC),
    /** Register and follow purchase orders. */
    MANAGE_PURCHASES("/compras/gestion", "Compras", "Gestionar compras", Role.GC),
    /** Maintain the supplier catalog. */
    MANAGE_SUPPLIERS("/compras/proveedores", "Compras", "Gestionar proveedores", Role.GC),
    /** Run the monthly payroll. */
    MANAGE_PAYROLL("/nominas/gestion", "Nóminas", "Gestionar nóminas", Role.GN),
    /** Maintain worker records. */
    MANAGE_WORKERS("/nominas/trabajadores", "Nóminas", "Gestionar trabajadores", Role.GN),
    /** Maintain customer records. */
    MANAGE_CUSTOMERS("/ventas/clientes", "Ventas", "Gestionar clientes", Role.GV),
    /** Issue and void invoices. */
    MANAGE_INVOICES("/ventas/facturas", "Ventas", "Gestionar facturas", Role.GV),
    /** Prepare quotes for customers. */
    MANAGE_QUOTES("/ventas/presupuestos", "Ventas", "Gestionar presupuestos", Role.GV);

    /** Context relative prefix shared by every module. */
    public static final String SECURE_PREFIX = "/secure";

    private final String path;
    private final String area;
    private final String title;
    private final Role role;

    Module(String path, String area, String title, Role role) {
        this.path = path;
        this.area = area;
        this.title = title;
        this.role = role;
    }

    /**
     * Finds the module served at a path.
     *
     * @param servletPathAndInfo path below {@link #SECURE_PREFIX}, such as {@code /ventas/clientes}
     * @return the module, or empty when no module lives there
     */
    public static Optional<Module> at(String servletPathAndInfo) {
        return Arrays.stream(values()).filter(m -> m.path.equals(servletPathAndInfo)).findFirst();
    }

    /**
     * Returns the context relative URL.
     *
     * @return such as {@code /secure/ventas/clientes}
     */
    public String url() {
        return SECURE_PREFIX + path;
    }

    /**
     * Returns the path below {@link #SECURE_PREFIX}.
     *
     * @return such as {@code /ventas/clientes}
     */
    public String path() {
        return path;
    }

    /**
     * Returns the menu group.
     *
     * @return area name in Spanish
     */
    public String area() {
        return area;
    }

    /**
     * Returns the page title.
     *
     * @return title in Spanish
     */
    public String title() {
        return title;
    }

    /**
     * Returns the only role allowed in.
     *
     * @return guarding role
     */
    public Role role() {
        return role;
    }
}
