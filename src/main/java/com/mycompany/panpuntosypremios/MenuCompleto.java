package com.mycompany.panpuntosypremios;

import java.math.BigDecimal;

public class MenuCompleto extends ItemVenta {
    private final long idMenu;

    public MenuCompleto(long idMenu, String codigo, String nombre, BigDecimal precio, int cantidad) {
        super(codigo, nombre, precio, cantidad);
        this.idMenu = idMenu;
    }

    public long getIdMenu() { return idMenu; }
    @Override public int calcularPuntos() { return getCantidad() * 8; }
    @Override public String getTipo() { return "MENU"; }
}
