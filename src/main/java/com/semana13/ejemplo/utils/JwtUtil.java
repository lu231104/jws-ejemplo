package com.semana13.ejemplo.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil { // La CLAVE SECRETA generada para HS256 (Debe ser estática y secreta)
    private final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 1. Método para FIRMAR (JWS) - Crea el Token
    public String generateToken(String username, List<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("authorities", roles)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30)) // 30 min
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256) // APLICACIÓN DE LA FIRMA JWS
                .compact();
    }

    // 2. Método para VERIFICAR LA INTEGRIDAD (JWS) - Prueba de Autenticidad
    public boolean validateToken(String token) {
        try {
            // Si la firma no coincide o está expirado, lanza excepción.
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            // Captura el fallo de Integridad JWS
            System.err.println("Fallo de Integridad JWS: El token fue manipulado.");
            return false;
        } catch (Exception e) {
            // Captura el fallo de expiración, etc.
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token).getBody().getSubject();
    }
}