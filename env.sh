#!/bin/bash

setenv PGPASSWORD wikibrain
setenv LC_ALL en_US.UTF-8
setenv LANG en_US.UTF-8
setenv LANGUAGE en_US.UTF-8

setenv CLASSPATH ""
setenv MAVEN_OPTS "-Dmaven.repo.local=/export/scratch/shilad/.m2"
setenv PATH "/export/scratch/shilad/apache-maven-3.2.1/bin:$PATH"
setenv JAVA_OPTS "-d64 -server -Xmx48G -da"
setenv WB_JAVA_OPTS "$JAVA_OPTS"
