# Distroless JRE 21 image (FUTURESTATE-LOWLEVEL §1). Jar built by Gradle bootJar.
# debian13: debian12 base carries open HIGH CVEs (libexpat et al.) the trivy
# CI gate rejects; debian13 is clean as of 2026-10.
FROM gcr.io/distroless/java21-debian13:nonroot
COPY demo-adapters/adapters-rest/build/libs/adapters-rest.jar /app/demo-svc.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/demo-svc.jar"]
