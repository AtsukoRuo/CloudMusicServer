FROM openjdk:17-slim
EXPOSE 30010
ARG JAR_FILE
ADD target/${JAR_FILE} /eureka-server.jar
ENTRYPOINT ["java", "-jar", "eureka-server.jar"]