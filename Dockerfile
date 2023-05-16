FROM alpine

WORKDIR /opt/robertify

ARG VERSION
COPY target/Robertify-${VERSION}-jar-with-dependencies.jar ./robertify.jar


RUN apk --no-cache add openjdk17-jre-headless
RUN apk --no-cache add ca-certificates

ARG KTOR_PORT
EXPOSE $KTOR_PORT

CMD java -Xmx1G -Xms1G -XX:+ShowCodeDetailsInExceptionMessages -XX:+CrashOnOutOfMemoryError -jar robertify.jar