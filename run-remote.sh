#!/bin/bash
GOOGLE_APPLICATION_CREDENTIALS='credentials.json' time mvn compile exec:java \
  -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
  -Dexec.args="--project=minecraft-seed-finder \
  --stagingLocation=gs://seed-finder/staging/ \
  --output=gs://seed-finder/output/seeds \
  --region=us-west1 \
  --runner=DataflowRunner \
  --jobName=minecraft-seed-finder \
  --start_seed=0 --end_seed=4G --woodland_mansions=2"
