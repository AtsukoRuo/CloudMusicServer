#Start with a base image containing Java runtime
FROM openjdk:17-slim
EXPOSE 30001
# The application's jar file
ARG JAR_FILE

# Add the application's jar to the container
ADD target/${JAR_FILE} /app.jar

# Execute the application
ENTRYPOINT ["java","-jar","/app.jar"]