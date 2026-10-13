# VULNERABLE: Insecure Dockerfile for demo purposes
FROM openjdk:17-jdk

# VULNERABLE: Running as root
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

# VULNERABLE: No health check defined
# VULNERABLE: Using full JDK instead of JRE

# VULNERABLE: Exposing unnecessary ports
EXPOSE 8080 8443 5005 9090

COPY target/vulnerable-demo-app-1.0.0.jar /app/app.jar

# VULNERABLE: No memory limits configured
# VULNERABLE: Running with debug agent enabled in production
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:8080/actuator/health || exit 1
