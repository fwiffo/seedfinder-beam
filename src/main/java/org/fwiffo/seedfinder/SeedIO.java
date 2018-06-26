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
	
	public static class AggregateSeeds
			extends DoFn<KV<Long, Iterable<SeedMetadata>>, SeedFamily> {
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

	public static class AddKeys extends DoFn<SeedFamily, KV<Long, SeedFamily>> {
		private final Counter countInput = Metrics.counter(
				AddKeys.class, "precomputed-seed-families-read");
		private final boolean emulateBug;

		public AddKeys() {
			this.emulateBug = false;
		}

		public AddKeys(boolean emulateBug) {
			this.emulateBug = emulateBug;
		}

		@ProcessElement
		public void ProcessElement(ProcessContext c) {
			SeedFamily family = c.element();
			if (!emulateBug || family.huts[3].x >= 0 && family.huts[3].z >= 0) {
				c.output(KV.of(family.baseSeed, family));
				countInput.inc();
			}
		}
	}

	public static class DeaggregateSeeds
			extends DoFn<KV<Long, SeedFamily>, KV<Long, SeedMetadata>> {
		private final Counter countInput = Metrics.counter(
				DeaggregateSeeds.class, "seed-families-deaggregated");
		private final Counter countExtracted = Metrics.counter(
				DeaggregateSeeds.class, "full-seeds-extracted");

		@ProcessElement
		public void processElement(ProcessContext c)  {
			SeedFamily family = c.element().getValue();
			for (Long fullSeed : family.fullSeeds.keySet()) {
				SeedMetadata seed = family.expanded(fullSeed, family.fullSeeds.get(fullSeed));
				c.output(KV.of(family.baseSeed, seed));
				countExtracted.inc();
			}
			countInput.inc();
		}
	}
}
