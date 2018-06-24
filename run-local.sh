#!/bin/bash
time mvn compile exec:java \
  -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
  -Dexec.args='--output=./output/seeds --start_seed=1G --end_seed=2G --spawn_biomes=jungle' \
  "$@"
