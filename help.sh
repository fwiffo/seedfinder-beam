#!/bin/bash
mvn compile exec:java \
  -Dexec.args="--help=org.fwiffo.seedfinder.SeedFinderPipeline\$SeedFinderOptions"
