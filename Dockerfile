FROM maven:3-eclipse-temurin-21 AS build-app
WORKDIR /build
COPY . .
RUN mvn package
RUN mv target/urlshortener-jar-with-dependencies.jar /app.jar

# https://github.com/docker-library/docs/blob/master/eclipse-temurin/README.md#creating-a-jre-using-jlink
FROM eclipse-temurin:21 AS build-jre
WORKDIR /build
COPY --from=build-app /app.jar .
RUN $JAVA_HOME/bin/jlink \
         --add-modules $($JAVA_HOME/bin/jdeps --ignore-missing-deps --print-module-deps app.jar) \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime

FROM debian:latest
WORKDIR /
COPY --from=build-jre /javaruntime /javaruntime
COPY --from=build-app /app.jar .
CMD ["/javaruntime/bin/java", "-jar", "app.jar"]