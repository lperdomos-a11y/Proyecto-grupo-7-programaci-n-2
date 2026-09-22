package com.mycompany.panpuntosypremios;

public class Cliente {
    private long id;
    private String identificador;
    private String nombre;
    private String telefono;
    private String correo;
    private int saldoPuntos;
    private boolean activo;

    public Cliente(long id, String identificador, String nombre, String telefono, String correo, int saldoPuntos, boolean activo) {
        this.id = id;
        this.identificador = identificador;
        this.nombre = nombre;
        this.telefono = telefono;
        this.correo = correo;
        this.saldoPuntos = saldoPuntos;
        this.activo = activo;
    }

    public long getId() { return id; }
    public String getIdentificador() { return identificador; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getCorreo() { return correo; }
    public int getSaldoPuntos() { return saldoPuntos; }
    public boolean isActivo() { return activo; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setCorreo(String correo) { this.correo = correo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    @Override public String toString() { return identificador + " - " + nombre + " (" + saldoPuntos + " pts)"; }
}
