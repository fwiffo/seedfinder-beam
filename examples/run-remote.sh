#!/bin/bash
GOOGLE_APPLICATION_CREDENTIALS='credentials.json' time mvn compile exec:java \
    -Dexec.args="--project=minecraft-seed-finder \
    --stagingLocation=gs://seed-finder/staging/ \
    --gcpTempLocation=gs://seed-finder/temp/ \
    --region=us-west1 \
    --numWorkers=40 \
    --runner=DataflowRunner \
    --jobName=minecraft-seed-finder-mushroom-0-20G \
    --start_seed=0G --end_seed=20G \
    --output=gs://seed-finder/output/mushroom-0-20G \
    --spawn_biomes=mushroom_island"
