#!/bin/bash
mvn compile
time mvn compile exec:java \
  -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
  -Dexec.args='--output=./output/seeds --start_seed=1000000000 --end_seed=2000000000 --ocean_spawn'
