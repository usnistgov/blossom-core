FROM gradle:5.6.2-jdk11

COPY / /asset
WORKDIR /asset
RUN ./gradlew getDeps --build-file build-libs.gradle
RUN ./gradlew shadowJar


