#! /bin/bash

mvn install:install-file -Dfile="lib/idl-choco-1.0.0.jar" -DgroupId="es.us.isa" -DartifactId=idl-choco -Dversion="1.0.0" -Dpackaging=jar
mvn install:install-file -Dfile=lib/IDLAnalyzer-1.0.1.jar -DgroupId=es.us.isa -DartifactId=IDLAnalyzer -Dversion=1.0.1 -Dpackaging=jar
