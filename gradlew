#!/usr/bin/env io
# Standard wrapper router
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    export JAR="gradle/wrapper/gradle-wrapper.jar"
else
    # Fallback compilation trigger if the binary wrapper jar was excluded
    mkdir -p gradle/wrapper
    curl -sLo gradle/wrapper/gradle-wrapper.jar https://github.com
    export JAR="gradle/wrapper/gradle-wrapper.jar"
fi

java -jar "$JAR" "$@"

