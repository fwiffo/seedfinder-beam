package org.fwiffo.seedfinder;

import java.util.Hashtable;
import java.util.Iterator;

import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import org.fwiffo.seedfinder.types.Location;
import org.fwiffo.seedfinder.types.SeedFamily;
import org.fwiffo.seedfinder.types.SeedMetadata;

public class SeedIO {
	// TODO: Functionality for ungrouping seed families read from a file.
	
	public static class AggregateSeeds extends DoFn<KV<Long, Iterable<SeedMetadata>>, SeedFamily> {
		private final Counter countFamilies = Metrics.counter(
				AggregateSeeds.class, "seed-families-aggregated");

		@ProcessElement
		public void processElement(ProcessContext c)  {
			long baseSeed = c.element().getKey();
			Iterable<SeedMetadata> allSeeds = c.element().getValue();

			Hashtable<Long, Location> fullSeeds = new Hashtable<Long, Location>();
			for (SeedMetadata seed : allSeeds) {
				fullSeeds.put(seed.seed, seed.spawn);
			}

			// All the seeds of the same key (family) have the same structures,
			// so we can just grab the witch hut locations from any one of them.
			Location[] huts = allSeeds.iterator().next().huts;
			c.output(new SeedFamily(baseSeed, fullSeeds, huts));
			countFamilies.inc();
		}
	}
}
