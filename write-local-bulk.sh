#!/bin/bash
time mvn compile exec:java \
    -Dexec.args=" \
    --output=./precomputed/25-50G/seeds \
    --start_seed=25G --end_seed=50G \
    --bulk_search_mode"
