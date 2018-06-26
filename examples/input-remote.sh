#!/bin/bash
GOOGLE_APPLICATION_CREDENTIALS='credentials.json' time mvn compile exec:java \
    -Dexec.args="--project=minecraft-seed-finder \
    --stagingLocation=gs://seed-finder/staging/ \
    --gcpTempLocation=gs://seed-finder/temp/ \
    --region=us-west1 \
    --numWorkers=40 \
    --runner=DataflowRunner \
    --jobName=minecraft-seed-finder-all-biomes-ocean-spawn \
    --input=gs://seed-finder/precomputed/0-200G/seeds* \
    --output=gs://seed-finder/output/all-biomes-ocean-spawn \
    --spawn_biomes=ocean \
    --all_biomes_nearby"
