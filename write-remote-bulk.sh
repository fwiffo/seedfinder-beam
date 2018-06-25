#!/bin/bash
GOOGLE_APPLICATION_CREDENTIALS='credentials.json' time mvn compile exec:java \
    -Dexec.args="--project=minecraft-seed-finder \
    --stagingLocation=gs://seed-finder/staging/ \
    --gcpTempLocation=gs://seed-finder/temp/ \
    --region=us-west1 \
    --numWorkers=40 \
    --runner=DataflowRunner \
    --jobName=minecraft-seed-finder-bulk-10-100G \
    --output=gs://seed-finder/precomputed/10-100G/seeds \
    --bulk_search_mode \
    --start_seed=10G --end_seed=100G"
