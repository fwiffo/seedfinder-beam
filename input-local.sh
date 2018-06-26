#!/bin/bash
time mvn compile exec:java \
    -Dexec.args=" \
    --input=precomputed/0-25G/seeds-* \
    --output=./output/ocean/seeds \
    --spawn_biomes=ocean"
