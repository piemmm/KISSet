#! /bin/bash


# This script is used to run kisset via maven, from intellij so we can debug it.

#mvn run:exec -Dexec.executable="java" -Dexec.args="-classpath %classpath com.kiss.kisset.KISSetMain"

mvn exec:java -Dexec.mainClass="com.kiss.kisset.KISSetMain"
