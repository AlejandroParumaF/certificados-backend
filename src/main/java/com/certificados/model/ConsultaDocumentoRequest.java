package com.certificados.model;

public class ConsultaDocumentoRequest {

    private String nit;
    private String anio;

    // Constructor (vacio)
    public ConsultaDocumentoRequest() {
    }

    // Constructor (con argumentos)
    public ConsultaDocumentoRequest(String nit, String anio) {
        this.nit = nit;
        this.anio = anio;
    }

    // Getters y setters
    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getAnio() {
        return anio;
    }

    public void setAnio(String anio) {
        this.anio = anio;
    }

}
