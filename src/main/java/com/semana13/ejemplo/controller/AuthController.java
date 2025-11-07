package com.semana13.ejemplo.controller;

import com.semana13.ejemplo.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthController {
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/auth/login")
    public ResponseEntity<String> login() {
        // Se asume validación exitosa. Procede a FIRMAR (JWS)
        String token = jwtUtil.generateToken("admin", List.of("ADMIN"));
        return ResponseEntity.ok("{\"token\": \"" + token + "\"}");
    }

    @GetMapping("/admin/reporte")
    public ResponseEntity<String> getAdminReport() {
        return ResponseEntity.ok("Acceso concedido por token JWS válido. Reporte Secreto.");
    }
}