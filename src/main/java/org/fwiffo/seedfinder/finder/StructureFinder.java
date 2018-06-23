package org.fwiffo.seedfinder.finder;

import java.lang.Math;
import java.lang.ThreadLocal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fwiffo.seedfinder.biome.Biome;
import org.fwiffo.seedfinder.biome.BiomeGenerator;
import org.fwiffo.seedfinder.util.Location;
import org.fwiffo.seedfinder.util.SeedMetadata;
import org.fwiffo.seedfinder.structure.WitchHut;

public class StructureFinder {
	private static final int SPAWN_SEARCH_RADIUS = 256;
	private static final ArrayList<Biome> VALID_SPAWN_BIOMES = new ArrayList<Biome>(
			Arrays.asList(
				Biome.forest,
				Biome.plains,
				Biome.taiga,
				Biome.taigaHills,
				Biome.forestHills,
				Biome.jungle,
				Biome.jungleHills));

	private static final ThreadLocal<WitchHut> threadWitchHut =
			ThreadLocal.withInitial(() -> new WitchHut());
	private static final Logger LOG = LoggerFactory.getLogger(StructureFinder.class);

	private static Location locateSpawn(long seed, BiomeGenerator generator) {
		Random random = new Random(seed);
		int radius = SPAWN_SEARCH_RADIUS >> 2;
		int size = radius * 2 + 1;

		int[] biomeData = generator.getBiomeData(-radius, -radius, size, size, true);
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

	public static class PotentialQuadHutFinder extends DoFn<Long, KV<Long, SeedMetadata>> {
		private static final int HUT_CLOSENESS = 2;
		private static final int MIN_EDGE = 1;
		private static final int MAX_EDGE = 22;
		public static final int BATCH_SIZE = 16384;

		// Radius to search, in regions. User specifies as blocks, and it's
		// rounded up to the nearest region.
		private final int radius;

		public PotentialQuadHutFinder() {
			this.radius = 4;
		}

		public PotentialQuadHutFinder(int radiusBlocks) {
			// Radius in blocks / 16 blocks per chunk / 32 chunks per region
			this.radius = (int)Math.ceil(
					(float)radiusBlocks / threadWitchHut.get().structureRegionSize / 16);
		}

		private SeedMetadata checkBaseSeed(WitchHut hut, long baseSeed) {
			// MC-131462 prevents East or South huts from spawning in negative
			// X/Z coordinates respecitvely. If it's fixed, this should search
			// negative coordinates starting at -radius.

			// As an optimiation, skips by two to find potential huts that could
			// be a member of a quad hut group while only checking 1 out of 4.
			for (int regionX=0; regionX < radius; regionX += (regionX < radius-2 ? 2 : 1)) {
				for (int regionZ=0; regionZ < radius; regionZ += (regionZ < radius-2 ? 2 : 1)) {
					Location check = hut.chunkLocationInRegionEdge(
							regionX, regionZ, baseSeed, HUT_CLOSENESS);
					if (check == null) {
						continue;
					}

					// If this region contains a witch hut that could be part
					// of a quad hut configuration, then start checking that
					// grouping from what would be the top left, based on this
					// first identified hut.
					int rx = check.x <= MIN_EDGE ? regionX-1 : regionX;
					int rz = check.z <= MIN_EDGE ? regionZ-1 : regionZ;

					Location topLeft = hut.chunkLocationInRegion(rx, rz, baseSeed);
					if (topLeft == null || topLeft.x < MAX_EDGE || topLeft.z < MAX_EDGE) {
						continue;
					}

					Location topRight = hut.chunkLocationInRegion(rx+1, rz, baseSeed);
					if (topRight == null || topRight.x > MIN_EDGE || topRight.z < MAX_EDGE) {
						continue;
					}

					Location bottomLeft = hut.chunkLocationInRegion(rx, rz+1, baseSeed);
					if (bottomLeft == null || bottomLeft.x < MAX_EDGE || bottomLeft.z > MIN_EDGE) {
						continue;
					}

					Location bottomRight = hut.chunkLocationInRegion(rx+1, rz+1, baseSeed);
					if (bottomRight == null || bottomRight.x > MIN_EDGE || bottomRight.z > MIN_EDGE) {
						continue;
					}

					Location[] huts = new Location[]{
						hut.fullLocation(rx, rz, topLeft),
						hut.fullLocation(rx+1, rz, topRight),
						hut.fullLocation(rx, rz+1, bottomLeft),
						hut.fullLocation(rx+1, rz+1, bottomRight),
					};
					return new SeedMetadata(baseSeed, huts);
				}
			}
			return null;
		}

		@ProcessElement
		public void processElement(ProcessContext c) {
			WitchHut hut = threadWitchHut.get();

			long startSeed = c.element() * BATCH_SIZE;
			long endSeed = (c.element() + 1) * BATCH_SIZE;

			for (long baseSeed=startSeed; baseSeed < endSeed; baseSeed++) {
				SeedMetadata result = checkBaseSeed(hut, baseSeed);
				if (result != null) {
					LOG.info(String.format("Checking bits with potential %d...", baseSeed));
					c.output(KV.of(baseSeed, result));
				}
			}
		}
	}

	public static class QuadHutVerifier extends DoFn<KV<Long, SeedMetadata>, KV<Long, SeedMetadata>> {
		private static boolean allHutsWillSpawn(
				BiomeGenerator generator, WitchHut hut, SeedMetadata seed) {
			for (Location location : seed.huts) {
				if (!hut.structureWillSpawn(location, generator)) {
					return false;
				}
			}
			return true;
		}

		@ProcessElement
		public void processElement(ProcessContext c) {
			WitchHut hut = threadWitchHut.get();
			SeedMetadata baseSeed = c.element().getValue();

			for (long high=0; high<1<<16; high++) {
				long fullSeed = (high<<48) ^ baseSeed.seed;
				BiomeGenerator generator = new BiomeGenerator(fullSeed, 2);
				if (allHutsWillSpawn(generator, hut, baseSeed)) {
					Location spawn = locateSpawn(fullSeed, generator);
					c.output(KV.of(baseSeed.seed, baseSeed.expanded(fullSeed, spawn)));
				}
			}
		}
	}
}
