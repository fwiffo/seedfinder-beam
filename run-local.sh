#!/bin/bash
time mvn compile exec:java \
    -Dexec.args=" \
    --output=./output/seeds \
    --start_seed=0 --end_seed=1G"
