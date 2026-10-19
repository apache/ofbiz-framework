# Distroless JRE 21 image (FUTURESTATE-LOWLEVEL §1). Jar built by Gradle bootJar.
FROM gcr.io/distroless/java21-debian12:nonroot
COPY demo-adapters/adapters-rest/build/libs/adapters-rest.jar /app/demo-svc.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/demo-svc.jar"]
