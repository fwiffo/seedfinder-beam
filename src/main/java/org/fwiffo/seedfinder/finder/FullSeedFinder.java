package org.fwiffo.seedfinder.finder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Random;

import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import org.fwiffo.seedfinder.biome.Biome;
import org.fwiffo.seedfinder.biome.BiomeGenerator;
import org.fwiffo.seedfinder.Constants;
import org.fwiffo.seedfinder.finder.BiomeSearchConfig;
import org.fwiffo.seedfinder.finder.SeedFinder;
import org.fwiffo.seedfinder.structure.OceanMonument;
import org.fwiffo.seedfinder.structure.WitchHut;
import org.fwiffo.seedfinder.structure.WoodlandMansion;
import org.fwiffo.seedfinder.types.Location;
import org.fwiffo.seedfinder.types.SeedFamily;
import org.fwiffo.seedfinder.types.SeedMetadata;

public class FullSeedFinder extends SeedFinder {
	private static final ArrayList<Biome> VALID_SPAWN_BIOMES = new ArrayList<Biome>(
			Arrays.asList(
				Biome.forest,
				Biome.plains,
				Biome.taiga,
				Biome.taigaHills,
				Biome.forestHills,
				Biome.jungle,
				Biome.jungleHills));

	private static Location locateSpawn(long seed, BiomeGenerator generator) {
		Random random = new Random(seed);
		// Quarter resolution biome sarch.
		int radius = Constants.LEGAL_SPAWN_RADIUS / 4;
		int size = radius * 2 + 1;

		int[] biomeData = generator.getQuarterResolutionBiomeData(-radius, -radius, size, size);
		int numberOfValidFound = 0;
		Location location = null;
		for (int i=0; i<size*size; i++) {
			Biome biome = Biome.biomes[biomeData[i]];
			if (!VALID_SPAWN_BIOMES.contains(biome)) continue;
			// Choose any of the valid spawn locations with equal probability.
			if (location != null && random.nextInt(numberOfValidFound+1) != 0) continue;

			location = new Location((i % size - radius) << 2, (i / size - radius) << 2);
			numberOfValidFound++;
		}
		if (location == null) {
			return new Location(0, 0);
		}
		return location;
	}

	public static class VerifyQuadHuts
			extends DoFn<KV<Long, SeedFamily>, KV<Long, SeedMetadata>> {

		private final Counter countSeedsChecked = Metrics.counter(
				VerifyQuadHuts.class, "s3-quad-huts-full-seeds-checked");
		private final Counter countSeedsFound = Metrics.counter(
				VerifyQuadHuts.class, "s3-quad-huts-full-seeds-verified");
		private final Distribution verifiedPerFamily = Metrics.distribution(
				VerifyQuadHuts.class, "s3-quad-huts-verified-per-family");

		private static boolean allHutsWillSpawn(
				BiomeGenerator generator, WitchHut hut, Location[] huts) {
			for (Location location : huts) {
				if (!hut.structureWillSpawn(location, generator)) {
					return false;
				}
			}
			return true;
		}

		@ProcessElement
		public void processElement(ProcessContext c) {
			WitchHut hut = threadWitchHut.get();
			SeedFamily family = c.element().getValue();

			int count = 0;
			for (long high=0; high<1<<16; high++) {
				long fullSeed = (high<<48) ^ family.baseSeed;
				BiomeGenerator generator = new BiomeGenerator(fullSeed);
				if (allHutsWillSpawn(generator, hut, family.huts)) {
					Location spawn = locateSpawn(fullSeed, generator);
					c.output(KV.of(family.baseSeed, family.expanded(fullSeed, spawn)));
					count++;
				}
			}
			countSeedsChecked.inc(1<<16);
			countSeedsFound.inc(count);
			verifiedPerFamily.update(count);
		}
	}

	public static class VerifyOceanMonuments
			extends DoFn<KV<Long, SeedMetadata>, KV<Long, SeedMetadata>> {

		private final Counter countSeedsChecked = Metrics.counter(
				VerifyOceanMonuments.class, "s4-monument-full-seeds-checked");
		private final Counter countSeedsFound = Metrics.counter(
				VerifyOceanMonuments.class, "s4-monument-full-seeds-verified");

		@ProcessElement
		public void processElement(ProcessContext c) {
			OceanMonument monument = threadOceanMonument.get();
			SeedMetadata seed = c.element().getValue();
			BiomeGenerator generator = new BiomeGenerator(seed.seed);

			countSeedsChecked.inc();
			for (Location location : seed.monuments) {
				if (monument.structureWillSpawn(location, generator)) {
					c.output(c.element());
					countSeedsFound.inc();
					return;
				}
			}
		}
	}

	public static class VerifyWoodlandMansions
			extends DoFn<KV<Long, SeedMetadata>, KV<Long, SeedMetadata>> {

		private final Counter countSeedsChecked = Metrics.counter(
				VerifyOceanMonuments.class, "s5-woodland-mansion-full-seeds-checked");
		private final Counter countSeedsFound = Metrics.counter(
				VerifyWoodlandMansions.class, "s5-woodland-mansion-full-seeds-verified");

		private final int minMansions;

		public VerifyWoodlandMansions(int minMansions) {
			this.minMansions = minMansions;
		}

		@ProcessElement
		public void processElement(ProcessContext c) {
			if (minMansions < 1) {
				c.output(c.element());
				return;
			}

			WoodlandMansion mansion = threadWoodlandMansion.get();
			SeedMetadata seed = c.element().getValue();
			BiomeGenerator generator = new BiomeGenerator(seed.seed);

			int mansionCount = 0;
			// TODO: Rather than return early, maybe we want to display
			// the mansion locations. If so, we'll need to update it with
			// the verified ones.
			countSeedsChecked.inc();
			for (Location location : seed.mansions) {
				if (mansion.structureWillSpawn(location, generator)) {
					mansionCount++;
					if (mansionCount >= minMansions) {
						c.output(c.element());
						countSeedsFound.inc();
						return;
					}
				}
			}
		}
	}

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

		private final Counter countSeedsChecked = Metrics.counter(
				HasSpawnBiomes.class, "s6-spawn-biomes-full-seeds-checked");
		private final Counter countSeedsFound = Metrics.counter(
				HasSpawnBiomes.class, "s6-spawn-biomes-full-seeds-verified");

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
			countSeedsChecked.inc();
		}
	}

	public static class HasAllBiomesNearby extends DoFn<KV<Long, SeedMetadata>, KV<Long, SeedMetadata>> {

		// Other biome types are common everywhere, so don't need to be included.
		private static final int[][] CHECKED_BIOMES = new int[][]{
			BiomeSearchConfig.FLOWER_FOREST.includeBiomes,
			BiomeSearchConfig.ICE_SPIKES.includeBiomes,
			BiomeSearchConfig.JUNGLE.includeBiomes,
			BiomeSearchConfig.MEGA_TAIGA.includeBiomes,
			BiomeSearchConfig.MESA.includeBiomes,
			BiomeSearchConfig.MUSHROOM_ISLAND.includeBiomes,
		};

		private final Counter countSeedsChecked = Metrics.counter(
				HasAllBiomesNearby.class, "s7-all-biomes-full-seeds-checked");
		private final Counter countSeedsFound = Metrics.counter(
				HasAllBiomesNearby.class, "s7-all-biomes-full-seeds-verified");
		private final int radius;

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
			countSeedsChecked.inc();
		}
	}
}
