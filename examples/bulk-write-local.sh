#!/bin/bash
# 8k batch was 553m43.703s
time mvn compile exec:java \
    -Dexec.args=" \
    --output=./precomputed/75-100G/seeds \
    --start_seed=75G --end_seed=100G \
    --bulk_search_mode"
