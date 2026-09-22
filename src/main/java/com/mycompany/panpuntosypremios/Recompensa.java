package com.mycompany.panpuntosypremios;

public record Recompensa(long id, String nombre, int puntosNecesarios, String tipoObjetivo, String productoCodigo, Long idMenu) {
    @Override public String toString() { return nombre + " - " + puntosNecesarios + " pts"; }
}
