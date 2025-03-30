package com.certificados.model;

public class EnvioDocumentoRequest {

    private String nit;
    private String nombre;
    private String email;
    private String anio;

    // Constructor (vacio)
    public EnvioDocumentoRequest(){}

    // Constructor (con argumentos)
    public EnvioDocumentoRequest(String nit, String nombre, String email, String anio) {
        this.nit = nit;
        this.nombre = nombre;
        this.email = email;
        this.anio = anio;
    }

    // Getters y setters
    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAnio() {
        return anio;
    }

    public void setAnio(String anio) {
        this.anio = anio;
    }
}
