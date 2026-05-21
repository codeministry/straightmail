FROM eclipse-temurin:21-jre

ADD build/libs/*.jar /straightmail.jar
ADD src/main/resources /resources

EXPOSE 50003 50004

ENTRYPOINT ["java", "-jar", "/straightmail.jar"]
