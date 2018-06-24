package org.fwiffo.seedfinder.finder;

import java.lang.Math;
import java.lang.ThreadLocal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fwiffo.seedfinder.Constants;
import org.fwiffo.seedfinder.biome.Biome;
import org.fwiffo.seedfinder.biome.BiomeGenerator;
import org.fwiffo.seedfinder.structure.OceanMonument;
import org.fwiffo.seedfinder.structure.WitchHut;
import org.fwiffo.seedfinder.structure.WoodlandMansion;
import org.fwiffo.seedfinder.util.Location;
import org.fwiffo.seedfinder.util.SeedFamily;
import org.fwiffo.seedfinder.util.SeedMetadata;

public class StructureFinder {
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
	private static final ThreadLocal<OceanMonument> threadOceanMonument =
			ThreadLocal.withInitial(() -> new OceanMonument());
	private static final ThreadLocal<WoodlandMansion> threadWoodlandMansion =
			ThreadLocal.withInitial(() -> new WoodlandMansion());

	private static final Logger LOG = LoggerFactory.getLogger(StructureFinder.class);

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

	public static class HasPotentialQuadHuts extends DoFn<Long, KV<Long, SeedFamily>> {
		private static final int HUT_CLOSENESS = 2;
		private static final int MIN_EDGE = 1;
		private static final int MAX_EDGE = 22;

		private final Counter countSeedsChecked = Metrics.counter(
				HasPotentialQuadHuts.class, "quad-huts-48bit-seeds-checked");
		private final Counter countPotentialFound = Metrics.counter(
				HasPotentialQuadHuts.class, "quad-huts-48bit-potenial-seeds-found");

		// Radius to search, in regions. User specifies as blocks, and it's
		// rounded up to the nearest region.
		private final int radius;

		public HasPotentialQuadHuts() {
			this.radius = 4;
		}

		public HasPotentialQuadHuts(int radiusBlocks) {
			// Radius in blocks / 16 blocks per chunk / 32 chunks per region
			this.radius = (int)Math.ceil(
					(float)radiusBlocks / threadWitchHut.get().structureRegionSize / 16);
		}

		private SeedFamily checkBaseSeed(WitchHut hut, long baseSeed) {
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
					return new SeedFamily(baseSeed, huts);
				}
			}
			return null;
		}

		@ProcessElement
		public void processElement(ProcessContext c) {
			WitchHut hut = threadWitchHut.get();

			long startSeed = c.element() * Constants.BATCH_SIZE;
			long endSeed = (c.element() + 1) * Constants.BATCH_SIZE;

			for (long baseSeed=startSeed; baseSeed < endSeed; baseSeed++) {
				SeedFamily result = checkBaseSeed(hut, baseSeed);
				if (result != null) {
					LOG.info(String.format("Checking bits with potential %d...", baseSeed));
					c.output(KV.of(baseSeed, result));
					countPotentialFound.inc();
				}
			}
			countSeedsChecked.inc(endSeed-startSeed);
		}
	}

	public static class VerifyQuadHuts
			extends DoFn<KV<Long, SeedFamily>, KV<Long, SeedMetadata>> {

		private final Counter countSeedsChecked = Metrics.counter(
				VerifyQuadHuts.class, "quad-huts-full-seeds-checked");
		private final Counter countSeedsFound = Metrics.counter(
				VerifyQuadHuts.class, "quad-huts-full-seeds-verified");

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

			for (long high=0; high<1<<16; high++) {
				long fullSeed = (high<<48) ^ family.baseSeed;
				BiomeGenerator generator = new BiomeGenerator(fullSeed);
				if (allHutsWillSpawn(generator, hut, family.huts)) {
					Location spawn = locateSpawn(fullSeed, generator);
					c.output(KV.of(family.baseSeed, family.expanded(fullSeed, spawn)));
					countSeedsFound.inc();
				}
			}
			countSeedsChecked.inc(1<<16);
		}
	}

	public static class HasPotentialOceanMonuments
			extends DoFn<KV<Long, SeedFamily>, KV<Long, SeedFamily>> {
		private final int closeness;
		private final int minEdge;
		private final int maxEdge;
		private final Counter countPotentialFound = Metrics.counter(
				HasPotentialOceanMonuments.class, "monument-48bit-potenial-seeds-found");

		public HasPotentialOceanMonuments(int closeness) {
			this.closeness = closeness;
			// The position of the ocean monument on the top and left of the
			// group will be relative to the potential witch hut locations,
			// since the range for monuments is bigger than for witch huts.
			// The potential locations of the two structure types are the
			// same for the right and bottom sides of the quad hut area.
			this.maxEdge = threadWitchHut.get().structurePosRange - closeness;
			this.minEdge = closeness - 1;
		}

		@ProcessElement
		public void processElement(ProcessContext c) {
			SeedFamily family = c.element().getValue();
			OceanMonument monument = threadOceanMonument.get();

			Location r = monument.fullLocationToRegion(family.huts[0]);
			Location topLeft = monument.chunkLocationInRegion(r.x, r.z, family.baseSeed);
			Location topRight = monument.chunkLocationInRegion(r.x+1, r.z, family.baseSeed);
			Location bottomLeft = monument.chunkLocationInRegion(r.x, r.z+1, family.baseSeed);
			Location bottomRight = monument.chunkLocationInRegion(r.x+1, r.z+1, family.baseSeed);

			ArrayList<Location> monumentList = new ArrayList<Location>(family.huts.length);
			if (topLeft != null && topLeft.x >= maxEdge && topLeft.z >= maxEdge) {
				monumentList.add(monument.fullLocation(r.x, r.z, topLeft));
			}
			if (topRight != null && topRight.x <= minEdge && topRight.z >= maxEdge) {
				monumentList.add(monument.fullLocation(r.x+1, r.z, topRight));
			}
			if (bottomLeft != null && bottomLeft.x >= maxEdge && bottomLeft.z <= minEdge) {
				monumentList.add(monument.fullLocation(r.x, r.z+1, bottomLeft));
			}
			if (bottomRight != null && bottomRight.x <= minEdge && bottomRight.z <= minEdge) {
				monumentList.add(monument.fullLocation(r.x+1, r.z+1, bottomRight));
			}

			if (monumentList.size() == 0) {
				return;
			}

			Location[] monuments = new Location[monumentList.size()];
			monuments = monumentList.toArray(monuments);
			c.output(KV.of(family.baseSeed, family.withMonuments(monuments)));
			countPotentialFound.inc();
		}
	}

	public static class VerifyOceanMonuments
			extends DoFn<KV<Long, SeedMetadata>, KV<Long, SeedMetadata>> {

		private final Counter countSeedsFound = Metrics.counter(
				VerifyOceanMonuments.class, "monument-full-seeds-verified");

		@ProcessElement
		public void processElement(ProcessContext c) {
			OceanMonument monument = threadOceanMonument.get();
			SeedMetadata seed = c.element().getValue();
			BiomeGenerator generator = new BiomeGenerator(seed.seed);

			for (Location location : seed.monuments) {
				if (monument.structureWillSpawn(location, generator)) {
					c.output(c.element());
					countSeedsFound.inc();
					return;
				}
			}
		}
	}

	public static class FindPotentialWoodlandMansions
			extends DoFn<KV<Long, SeedFamily>, KV<Long, SeedFamily>> {
		// Radius to search, in regions. User specifies as blocks, and it's
		// rounded up to the nearest region.
		private final int radius;

		public FindPotentialWoodlandMansions() {
			this.radius = 3;
		}

		public FindPotentialWoodlandMansions(int radiusBlocks) {
			// Radius in blocks / 16 blocks per chunk / 32 chunks per region
			this.radius = (int)Math.ceil(
					(float)radiusBlocks / threadWoodlandMansion.get().structureRegionSize / 16);
		}

		@ProcessElement
		public void processElement(ProcessContext c) {
			SeedFamily family = c.element().getValue();
			ArrayList<Location> mansionLocations = new ArrayList<Location>(radius*radius*4);
			WoodlandMansion mansion = threadWoodlandMansion.get();

			for (int regionX=-radius; regionX < radius; regionX++) {
				for (int regionZ=-radius; regionZ < radius; regionZ++) {
					Location m = mansion.chunkLocationInRegion(regionX, regionZ, family.baseSeed);
					if (m != null) {
						mansionLocations.add(mansion.fullLocation(regionX, regionZ, m));
					}
				}
			}
			Location[] mansions = new Location[mansionLocations.size()];
			mansions = mansionLocations.toArray(mansions);

			c.output(KV.of(family.baseSeed, family.withMansions(mansions)));
		}
	}

	public static class VerifyWoodlandMansions
			extends DoFn<KV<Long, SeedMetadata>, KV<Long, SeedMetadata>> {

		private final Counter countSeedsFound = Metrics.counter(
				VerifyWoodlandMansions.class, "woodland-mansion-full-seeds-verified");

		private final int minMansions;

		public VerifyWoodlandMansions() {
			this.minMansions = 1;
		}

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

}
