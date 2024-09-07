FROM nimbleplatform/nimble-base
MAINTAINER Salzburg Research <nimble-srfg@salzburgresearch.at>
VOLUME /tmp
ARG finalName
ENV JAR '/'$finalName
ARG port
ADD $finalName $JAR
RUN touch $JAR

ENTRYPOINT ["java", "-jar", "app.jar"]