#!/bin/bash
mvn compile
time mvn compile exec:java \
  -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
  -Dexec.args='--output=./output/seeds --start_seed=1G --end_seed=2G --woodland_mansions=2' \
  "$@"
