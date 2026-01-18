#!/bin/bash

# Script to switch between Minecraft versions
# Usage: ./switch-version.sh 1.21.10 or ./switch-version.sh 1.21.11

VERSION=$1

if [ "$VERSION" = "1.21.10" ]; then
    echo "Switching to Minecraft 1.21.10..."
    cp gradle.properties.1.21.10 gradle.properties
    cp build.gradle.1.21.10 build.gradle
    echo "Switched to 1.21.10. Run ./gradlew clean build"
elif [ "$VERSION" = "1.21.11" ]; then
    echo "Switching to Minecraft 1.21.11..."
    cp gradle.properties gradle.properties.1.21.11.backup 2>/dev/null || true
    cp build.gradle build.gradle.1.21.11.backup 2>/dev/null || true
    # Current files are already for 1.21.11
    echo "Already on 1.21.11. Run ./gradlew clean build"
else
    echo "Usage: $0 [1.21.10|1.21.11]"
    exit 1
fi
