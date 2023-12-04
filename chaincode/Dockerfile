FROM gradle:7-jdk11-alpine
ARG CC_NAME

COPY /$CC_NAME /$CC_NAME
WORKDIR /$CC_NAME
RUN gradle installDist
