FROM gradle:5.6.2-jdk11

COPY / /authorization
WORKDIR /authorization
RUN ./gradlew getDeps --build-file build-libs.gradle
RUN ./gradlew shadowJar


