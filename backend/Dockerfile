FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY build/libs/straightmail-*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd -r straightmail && useradd -r -g straightmail straightmail

COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/spring-boot-loader/ ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/application/ ./

RUN chown -R straightmail:straightmail /app

USER straightmail

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
