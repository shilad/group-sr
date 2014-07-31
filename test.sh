#!/bin/sh

(cd ../wikibrain && mvn install -DskipTests=true) &&
 mvn clean compile exec:java \
    -Dexec.mainClass=org.shilad.groupsr.Test \
    -Dexec.arguments="-c `pwd`/../wikibrain/psql.conf"