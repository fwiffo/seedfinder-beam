#!/bin/bash
time mvn compile exec:java \
  -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
  -Dexec.args="--output=./output/seeds \
  --start_seed=2G --end_seed=3G \
  --all_biomes_nearby"
