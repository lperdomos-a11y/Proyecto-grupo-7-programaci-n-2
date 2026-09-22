package com.mycompany.panpuntosypremios;

import java.math.BigDecimal;

public class ProductoIndividual extends ItemVenta {
    private final String categoria;

    public ProductoIndividual(String codigo, String nombre, BigDecimal precio, int cantidad, String categoria) {
        super(codigo, nombre, precio, cantidad);
        this.categoria = categoria;
    }

    public String getCategoria() { return categoria; }
    @Override public int calcularPuntos() { return "SANDWICH".equals(categoria) ? getCantidad() * 2 : 0; }
    @Override public String getTipo() { return "PRODUCTO"; }
}
