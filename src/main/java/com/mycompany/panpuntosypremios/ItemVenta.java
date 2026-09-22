package com.mycompany.panpuntosypremios;

import java.math.BigDecimal;

public abstract class ItemVenta {
    private final String codigo;
    private final String nombre;
    private final BigDecimal precio;
    private int cantidad;

    protected ItemVenta(String codigo, String nombre, BigDecimal precio, int cantidad) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public BigDecimal getSubtotal() { return precio.multiply(BigDecimal.valueOf(cantidad)); }
    public abstract int calcularPuntos();
    public abstract String getTipo();
    @Override public String toString() { return codigo + " - " + nombre + " - Q" + precio; }
}
