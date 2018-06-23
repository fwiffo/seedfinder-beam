package org.fwiffo.seedfinder.finder;

import java.lang.ThreadLocal;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fwiffo.seedfinder.biome.BiomeGenerator;
import org.fwiffo.seedfinder.util.Location;
import org.fwiffo.seedfinder.util.SeedMetadata;
import org.fwiffo.seedfinder.structure.WitchHut;

public class StructureFinder {
	// TODO: Make this a configuration option
	private static final int radius = 4;
	private static final ThreadLocal<WitchHut> threadWitchHut =
			ThreadLocal.withInitial(() -> new WitchHut());
	private static final Logger LOG = LoggerFactory.getLogger(StructureFinder.class);

	public static class PotentialQuadHutFinder extends DoFn<Long, KV<Long, SeedMetadata>> {
		private static final int HUT_CLOSENESS = 2;
		private static final int MIN_EDGE = 1;
		private static final int MAX_EDGE = 22;
		public static final int BATCH_SIZE = 16384;

		private static SeedMetadata checkBaseSeed(WitchHut hut, long baseSeed) {
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
		private static boolean allHutsWillSpawn(long fullSeed, WitchHut hut, SeedMetadata s) {
			BiomeGenerator generator = new BiomeGenerator(fullSeed, 2);
			for (Location location : s.getHuts()) {
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
				if (allHutsWillSpawn(fullSeed, hut, baseSeed)) {
					c.output(KV.of(fullSeed, baseSeed.withFullSeed(fullSeed)));
				}
			}
		}
	}
}
