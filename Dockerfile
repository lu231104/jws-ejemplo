# Usar imagen base de Maven para compilar el proyecto
FROM maven:3.9.6-eclipse-temurin-21 AS build
# Establecer el directorio de trabajo
WORKDIR /app
# Copiar todos los archivos del proyecto (pom.xml, src, etc.) a la imagen
COPY . .
# Ejecutar Maven para compilar el proyecto (se asume que el archivo jar se generará en target)
RUN mvn -B -DskipTests=true clean package
# Usar imagen de Java para ejecutar la aplicación
FROM eclipse-temurin:21-jre
# Establecer el directorio de trabajo
WORKDIR /app
# Crear el directorio /app/notas/data si es necesario
RUN mkdir -p /app/notas/data
# Copiar el archivo .jar generado en la etapa de construcción al contenedor
COPY --from=build /app/target/*.jar app.jar
# Exponer el puerto en el que la aplicación escuchará
EXPOSE 8080
# Comando para ejecutar la aplicación, con soporte para cambiar el puerto mediante la variable de entorno
CMD ["sh", "-c", "mkdir -p /app/notas/data && java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
