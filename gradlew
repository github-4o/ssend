#!/bin/sh
# Gradle wrapper stub - download actual wrapper or run: gradle wrapper
echo "Downloading gradle wrapper..."
if ! command -v gradle &> /dev/null; then
    echo "Error: gradle not found. Please install gradle or download wrapper files manually."
    exit 1
fi
gradle wrapper --gradle-version 8.7
./gradlew "$@"
