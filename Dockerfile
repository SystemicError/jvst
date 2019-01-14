FROM openjdk:8-alpine

COPY target/uberjar/jvst.jar /jvst/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/jvst/app.jar"]
