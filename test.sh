#! /usr/bin/env bash


docker compose down
rm -rf repositories/
docker compose up -d
mvn clean compile spring-boot:run
