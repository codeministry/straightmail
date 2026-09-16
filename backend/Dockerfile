FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build
COPY build/libs/straightmail-*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd -r straightmail && useradd -r -g straightmail straightmail

COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

RUN chown -R straightmail:straightmail /app

USER straightmail

EXPOSE 50003 50004

ENTRYPOINT ["java", "-jar", "application.jar"]
