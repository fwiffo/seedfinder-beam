package org.fwiffo.seedfinder.structure;

import java.util.Arrays;
import java.util.HashSet;
import org.fwiffo.seedfinder.biome.*;
import org.fwiffo.seedfinder.types.Location;

abstract public class SizedStructure extends Structure {
	private final int structureSize;

	public SizedStructure(
			long structureSeed, int structurePosRange, int structureRegionSize,
			Biome[] validBiomes, int structureSize) {
		super(structureSeed, structurePosRange, structureRegionSize, validBiomes);
		this.structureSize = structureSize;
	}

	// Ocean monuments and Woodland Mansions average two randInts, other
	// structures just use one.
	protected int getChunkInRegion() {
		return (rnd.nextInt(structurePosRange) + rnd.nextInt(structurePosRange)) / 2;
	}

	protected boolean areaHasValidBiomes(
			BiomeGenerator generator, Location location, int size, HashSet<Integer> checkBiomes) {
		int centerX = location.x;
		int centerZ = location.z;
		int left = (centerX - size) >> 2;
		int top = (centerZ - size) >> 2;
		int right = (centerX + size) >> 2;
		int bottom = (centerZ + size) >> 2;
		int width = right - left + 1;
		int height = bottom - top + 1;

		int[] biomes = generator.getQuarterResolutionBiomeData(left, top, width, height);
		for (int i=0; i<width*height; i++) {
			if (!checkBiomes.contains(biomes[i])) {
				return false;
			}
		}
		return true;
	}

	public boolean structureWillSpawn(Location location, BiomeGenerator generator) {
		int biomeAt = generator.getBiomeAt(location.x, location.z);
		if (!validBiomes.contains(biomeAt)) {
			return false;
		}
		return areaHasValidBiomes(generator, location, structureSize, validBiomes);
	}
}
