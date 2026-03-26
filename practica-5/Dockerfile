FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew shadowJar --no-daemon

EXPOSE 7000

CMD ["java", "-jar", "build/libs/app.jar"]