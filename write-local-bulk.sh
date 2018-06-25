#!/bin/bash
time mvn compile exec:java \
    -Dexec.args=" \
    --output=./precomputed/seeds \
    --start_seed=0 --end_seed=10G \
    --bulk_search_mode"
