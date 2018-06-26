#!/bin/bash
time mvn compile exec:java \
    -Dexec.args=" \
    --output=./output/woodland-mansions-2/seeds \
    --start_seed=0 --end_seed=1G
    --woodland_mansions=2"
