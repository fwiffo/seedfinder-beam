#!/bin/bash
GOOGLE_APPLICATION_CREDENTIALS='credentials.json' time mvn compile exec:java \
    -Dexec.args="--project=minecraft-seed-finder \
    --stagingLocation=gs://seed-finder/staging/ \
    --gcpTempLocation=gs://seed-finder/temp/ \
    --region=us-west1 \
    --runner=DataflowRunner \
    --jobName=minecraft-seed-finder-all-biomes-0-10G \
    --input=gs://seed-finder/precomputed/0-10G/seeds-* \
    --output=gs://seed-finder/output/all-biomes/0-10G/seeds \
    --all_biomes_nearby"
