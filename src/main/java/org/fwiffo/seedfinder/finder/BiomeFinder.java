package org.fwiffo.seedfinder.finder;

import java.lang.ThreadLocal;
import java.util.Hashtable;
import java.util.Random;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fwiffo.seedfinder.biome.Biome;
import org.fwiffo.seedfinder.biome.BiomeGenerator;
import org.fwiffo.seedfinder.util.SeedMetadata;

public class BiomeFinder {
	private static final Logger LOG = LoggerFactory.getLogger(BiomeFinder.class);

	private static final int SPAWN_SIZE = 256;
	private static final int SPAWN_AREA = 256*256;

	private static final float WATER_FRACTION = 0.80f;
	public static final Biome[] WATER_BIOMES = {
		Biome.ocean,
		Biome.deepOcean,
		Biome.frozenOcean,
		Biome.oceanM,
		Biome.deepOceanM,
		Biome.frozenOceanM,
	};

	private static boolean hasBiomes(
			BiomeGenerator generator, int size, int centerX, int centerZ,
			Biome[] biomes, Biome[] excluded, float minFraction) {
		Hashtable<Integer, Float> census = generator.biomeCensus(
				centerX, centerZ, size, size, true);

		float exFraction = 0f;
		if (excluded != null) {
			for (Biome ex : excluded) {
				exFraction += census.get(ex.index);
			}
		}
		if (exFraction > 0.90f) { exFraction = 0.90f; }

		float fraction = 0f;
		for (Biome biome : biomes) {
			fraction += census.get(biome.index);
		}

		return fraction / (1f - exFraction) >= minFraction;
	}

	private static boolean hasBiomes(
			BiomeGenerator generator, int size, Biome[] biomes, float minFraction) {
		return hasBiomes(generator, size, 0, 0, biomes, null, minFraction);
	}

	private static boolean hasBiomes(
			BiomeGenerator generator, int size, int centerX, int centerZ,
			Biome[] biomes, float minFraction) {
		return hasBiomes(generator, size, centerX, centerZ, biomes, null, minFraction);
	}

	private static boolean hasBiomes(
			BiomeGenerator generator, int size,
			Biome[] biomes, Biome[] excluded, float minFraction) {
		return hasBiomes(generator, size, 0, 0, biomes, excluded, minFraction);
	}

	public static class HasMostlyOceanSpawn extends DoFn<KV<Long, SeedMetadata>, KV<Long, SeedMetadata>> {
		@ProcessElement
		public void processElement(ProcessContext c) {
			SeedMetadata seed = c.element().getValue();
			BiomeGenerator generator = new BiomeGenerator(seed.seed);

			if (hasBiomes(generator, SPAWN_SIZE, seed.spawn.x, seed.spawn.z,
						WATER_BIOMES, WATER_FRACTION)) {
				c.output(c.element());
			}
		}
	}
}
