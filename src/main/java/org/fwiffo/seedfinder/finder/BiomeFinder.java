package org.fwiffo.seedfinder.finder;

import java.lang.ThreadLocal;
import java.util.Hashtable;
import java.util.Random;

import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fwiffo.seedfinder.biome.Biome;
import org.fwiffo.seedfinder.biome.BiomeGenerator;
import org.fwiffo.seedfinder.Constants;
import org.fwiffo.seedfinder.finder.BiomeSearchConfig;
import org.fwiffo.seedfinder.util.Location;
import org.fwiffo.seedfinder.util.SeedMetadata;

public class BiomeFinder {
	private static boolean hasBiomes(
			BiomeGenerator generator, Location center, int radius, BiomeSearchConfig config) {
		Hashtable<Integer, Float> census = generator.biomeCensus(center.x, center.z, radius);

		float ignoreFraction = 0f;
		for (int ignore : config.ignoreBiomes) {
			ignoreFraction += census.get(ignore);
		}
		if (ignoreFraction > 0.80f) { ignoreFraction = 0.80f; }

		float includeFraction = 0f;
		for (int include : config.includeBiomes) {
			includeFraction += census.get(include);
		}

		return includeFraction / (1f - ignoreFraction) >= config.minFraction;
	}

	public static class HasSpawnBiomes extends DoFn<KV<Long, SeedMetadata>, KV<Long, SeedMetadata>> {

		private final Counter countSeedsFound = Metrics.counter(
				HasSpawnBiomes.class, "spawn-biomes-full-seeds-verified");

		private final BiomeSearchConfig config;

		public HasSpawnBiomes(BiomeSearchConfig config) {
			this.config = config;
		}

		@ProcessElement
		public void processElement(ProcessContext c) {
			SeedMetadata seed = c.element().getValue();
			BiomeGenerator generator = new BiomeGenerator(seed.seed);

			if (hasBiomes(generator, seed.spawn, Constants.SPAWN_CHUNK_RADIUS, config)) {
				c.output(c.element());
				countSeedsFound.inc();
			}
		}
	}

	public static class HasAllBiomesNearby extends DoFn<KV<Long, SeedMetadata>, KV<Long, SeedMetadata>> {

		private static final int[][] CHECKED_BIOMES = new int[][]{
			BiomeSearchConfig.FLOWER_FOREST.includeBiomes,
			BiomeSearchConfig.ICE_SPIKES.includeBiomes,
			BiomeSearchConfig.JUNGLE.includeBiomes,
			BiomeSearchConfig.MEGA_TAIGA.includeBiomes,
			BiomeSearchConfig.MESA.includeBiomes,
			BiomeSearchConfig.MUSHROOM_ISLAND.includeBiomes,
		};

		private final Counter countSeedsFound = Metrics.counter(
				HasAllBiomesNearby.class, "all-biomes-full-seeds-verified");
		private final int radius;

		public HasAllBiomesNearby() {
			this.radius = 2048;
		}

		public HasAllBiomesNearby(int radius) {
			this.radius = radius;
		}

		private boolean hasAllBiomes(SeedMetadata seed) {
			BiomeGenerator generator = new BiomeGenerator(seed.seed);
			Hashtable<Integer, Float> census = generator.biomeCensus(
					seed.spawn.x, seed.spawn.z, radius);

			for (int[] biomeGroup : CHECKED_BIOMES) {
				float totalArea = 0f;
				for (int biome : biomeGroup) {
					totalArea += census.get(biome);
				}
				if (totalArea < 0.0005) { // 0.05%, or about 8 chunks with 2048 radius.
					return false;
				}
			}
			return true;
		}

		@ProcessElement
		public void processElement(ProcessContext c) {
			SeedMetadata seed = c.element().getValue();
			if (hasAllBiomes(seed)) {
				c.output(c.element());
				countSeedsFound.inc();
			}
		}
	}
}