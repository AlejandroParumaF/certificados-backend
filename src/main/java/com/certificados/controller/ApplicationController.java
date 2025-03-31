package com.certificados.controller;

import com.certificados.model.ConsultaDocumentoRequest;
import com.certificados.model.EnvioDocumentoRequest;
import com.certificados.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    @Autowired
    private ApplicationService ftpService;

    @GetMapping
    public ResponseEntity<String> getOpen(){
        return ResponseEntity.ok("Working");
    }

    @PostMapping("/consultar")
    public Map<String, Integer> consultarCertificados(@RequestBody ConsultaDocumentoRequest consultaRequest) throws IOException {
        int count = ftpService.getDocumentCount(consultaRequest.getAnio(), consultaRequest.getNit());

        return Collections.singletonMap("count", count);
    }

    @PostMapping("/enviar")
    public ResponseEntity<String> enviarCertificados(@RequestBody EnvioDocumentoRequest envioRequest) throws IOException {
        ftpService.enviarCorreo(envioRequest.getAnio(), envioRequest.getNit(), envioRequest.getNombre(), envioRequest.getEmail());
        return new ResponseEntity<>("Se enviaron los certificados al correo: " + envioRequest.getEmail(), HttpStatus.OK);
    }

    @GetMapping("/status")
    public String getStatus() {
        return "Working";
    }
}
