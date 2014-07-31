#!/bin/sh

for metric in milnewitten ESA ensemble; do
    mvn clean compile exec:java \
        -Dexec.mainClass=org.shilad.groupsr.BasicEvaluation \
        -Dexec.arguments="-c en.conf -l en -m $metric"
done
