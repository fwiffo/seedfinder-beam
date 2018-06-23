#!/bin/bash
GOOGLE_APPLICATION_CREDENTIALS='credentials.json' mvn compile exec:java \
  -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
  -Dexec.args="--project=minecraft-seed-finder \
  --stagingLocation=gs://minecraft-seed-finder/staging/ \
  --output=gs://corded-shard-208105/output \
  --runner=DataflowRunner \
  --jobName=minecraft-seedfinder" \
  -Dexec.args='--output=./output/seeds --start_seed=1000000000 --end_seed=2000000000 --woodland_mansions=2' \
  "$@"
