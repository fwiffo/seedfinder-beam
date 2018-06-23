#!/bin/bash
mvn compile
time mvn compile exec:java \
  -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
  -Dexec.args='--output=./output/seeds'
