package com.mycompany.panpuntosypremios;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PedidoResumen(long id, String cliente, LocalDateTime fecha, String estado, BigDecimal total, int puntos) {
    @Override public String toString() { return "#" + id + " - " + cliente + " - Q" + total + " - " + estado; }
}
