package com.certificados.controller;

import com.certificados.model.ConsultaDocumentoRequest;
import com.certificados.model.EnvioDocumentoRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    @GetMapping
    public ResponseEntity<String> sayHello(){
        return ResponseEntity.ok("WELCOME TO FULL STACK JAVA DEVELOPER, PUNE");
    }

    @PostMapping("/consultar")
    public Map<String, Integer> consultarCertificados(@RequestBody ConsultaDocumentoRequest consultaRequest) throws IOException {

        return Collections.singletonMap("count", 1);
    }

    @PostMapping("/enviar")
    public ResponseEntity<String> enviarCertificados(@RequestBody EnvioDocumentoRequest envioRequest) throws IOException {

        return new ResponseEntity<>("Se enviaron los certificados al correo: " + envioRequest.getEmail(), HttpStatus.OK);
    }

    @GetMapping("/status")
    public String getStatus() {
        return "Working";
    }
}
