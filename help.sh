#!/bin/bash
mvn compile exec:java \
  -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
  -Dexec.args="--help=org.fwiffo.seedfinder.SeedFinderPipeline\$SeedFinderOptions"
