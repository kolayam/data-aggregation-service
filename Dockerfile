FROM maven
WORKDIR /usr/src/app
COPY . .
RUN git clone https://github.com/kolayam/common.git
WORKDIR /usr/src/app/common
RUN mvn clean install -DskipTests
WORKDIR /usr/src/app
RUN mvn clean install -DskipTests
FROM openjdk:8
WORKDIR /usr/src/app
COPY --from=0 /usr/src/app/data-aggregation-service/target/app.jar ./
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar", "./app.jar"]