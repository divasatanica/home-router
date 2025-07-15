# Use an official OpenJDK runtime as a parent image
FROM openjdk:18

ARG VERSION
ENV JAR_VERSION=$VERSION

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file into the container at /app
COPY ./target/router-*.jar /app/router.jar

# Make port 8088 available to the world outside this container
EXPOSE 8088

# Run the application
CMD ["java", "-jar", "router.jar"]