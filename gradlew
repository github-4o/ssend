#!/usr/bin/env bash
# Standard execution router for GitHub runner environments

CLK_TCK=100
java -jar gradle/wrapper/gradle-wrapper.jar "$@"
