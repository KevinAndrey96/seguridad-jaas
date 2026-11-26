# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q -DskipTests package

FROM tomcat:11.0-jre21
RUN rm -rf "$CATALINA_HOME"/webapps/* \
    && useradd --system --uid 10001 --home-dir "$CATALINA_HOME" app \
    && chown -R app "$CATALINA_HOME"/webapps "$CATALINA_HOME"/work "$CATALINA_HOME"/temp "$CATALINA_HOME"/logs
COPY --from=build --chown=app /src/target/rbac.war "$CATALINA_HOME"/webapps/ROOT.war
ENV CATALINA_OPTS="-Dusers.file=/config/users.xml"
USER app
EXPOSE 8080
CMD ["catalina.sh", "run"]
