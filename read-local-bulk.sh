#!/bin/bash
time mvn compile exec:java \
    -Dexec.args=" \
    --input=precomputed/seeds-*.avro \
    --output=./output/seeds \
    --spawn_biomes=mushroom_island"
