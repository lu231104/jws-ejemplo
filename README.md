# Seguir los siguientes pasos
Desarrollo Práctico: Implementación JWS con Spring Boot 

Paso 1: Configuración Inicial del Proyecto (Setup Detallado y Completo)
1.	Herramientas: Necesitas JDK 17+ y un IDE (IntelliJ o VS Code).
2.	Spring Initializr: Crear un proyecto con Spring Web y Spring Security.
3.	Añadir JJWT (Manual): Agregar estas dependencias al archivo pom.xml (dentro de la etiqueta <dependencies>) para habilitar la funcionalidad JWS:
XML
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version> 
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
 Paso 2: Utilería JWT (JwtUtil.java) - Firma JWS
•	Archivo: Crear src/main/java/com/seguridad/jwtdemo/util/JwtUtil.java.


package com.seguridad.jwtdemo.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    // La CLAVE SECRETA generada para HS256 (Debe ser estática y secreta)
    private final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256); 

    // 1. Método para FIRMAR (JWS) - Crea el Token
    public String generateToken(String username, List<String> roles) {
        return Jwts.builder()
            .setSubject(username)
            .claim("authorities", roles)
            .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30)) // 30 min
            .signWith(SECRET_KEY, SignatureAlgorithm.HS256) //  APLICACIÓN DE LA FIRMA JWS
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
Paso 3: El Filtro de Peticiones (JwtRequestFilter.java)
 Crear src/main/java/com/seguridad/jwtdemo/config/JwtRequestFilter.java.

package com.seguridad.jwtdemo.config;

import com.seguridad.jwtdemo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7); // Extraer el token (después de "Bearer ")
        }

        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(jwt)) { // LLAMADA A LA PRUEBA DE INTEGRIDAD JWS

                String username = jwtUtil.getUsernameFromToken(jwt);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority("ADMIN")));

                // Si el JWS es válido, establecemos la identidad en el contexto
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
 Paso 4: Configuración Stateless (SecurityConfig.java)
 Crear src/main/java/com/seguridad/jwtdemo/config/SecurityConfig.java.

package com.seguridad.jwtdemo.config;

import org.springframework.beans.factory.annotation.Autowired;
// ... otros imports ...
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Desactivar CSRF para APIs REST
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN") // Requiere la claim 'ADMIN'
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                // CRUCIAL: Desactiva las sesiones de Spring para usar solo tokens JWS
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) 
            )
            // INYECTAR NUESTRO FILTRO ANTES DEL FILTRO DE AUTENTICACIÓN POR DEFECTO
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
Paso 5: Controlador (AuthController.java)
Crear src/main/java/com/seguridad/jwtdemo/controller/AuthController.java.

package com.seguridad.jwtdemo.controller;

import com.seguridad.jwtdemo.util.JwtUtil;
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
Paso 6: Prueba de Integridad (Postman)

•	Flujo de Prueba Explícito:
1.	Ejecutar la Aplicación Spring Boot.
2.	Paso 1: Obtener JWS Válido (Postman). Hacer POST a http://localhost:8080/api/auth/login. Copiar el token generado.
3.	Paso 2: Manipulación (Ataque MITM). Copiar el token y cambiar manualmente un solo carácter en el Payload (la sección del medio, entre los dos puntos).
4.	Paso 3: Envío de Token Falso. Enviar el token manipulado a GET http://localhost:8080/api/admin/reporte con el header Authorization: Bearer [TOKEN MANIPULADO].
•	Resultado Esperado: 401 Unauthorized (No Autorizado).
•	Explicación: El mecanismo JWS detectó la manipulación porque el Hash del Payload modificado no coincidió con la firma original, probando que el token es fidedigno.

