#!/usr/bin/env sh

domain="org.gradle.wrapper"
CLASS_NAME="GradleWrapperMain"
DIR="$(cd "$(dirname "$0")" && pwd)"

if [ -d "$DIR/.gradle" ]; then
    GRADLE_USER_HOME="$DIR/.gradle"
    export GRADLE_USER_HOME
fi

if [ -d "$DIR/gradle/wrapper" ]; then
    WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
    WRAPPER_PROPERTIES="$DIR/gradle/wrapper/gradle-wrapper.properties"
    WRAPPER_MD5="$(cat "$WRAPPER_PROPERTIES" | grep distributionSha256Sum | cut -d= -f2)"
else
    echo "Error: Could not find gradle wrapper files."
    exit 1
fi

if [ -z "$JAVA_HOME" ]; then
    JAVA="java"
else
    JAVA="$JAVA_HOME/bin/java"
fi

"$JAVA" "-Dorg.gradle.appname=gradlew" -classpath "$WRAPPER_JAR" "$domain.$CLASS_NAME" "$@"