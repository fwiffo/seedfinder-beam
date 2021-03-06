package org.fwiffo.seedfinder.structure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import org.fwiffo.seedfinder.biome.*;
import org.fwiffo.seedfinder.types.Location;

public class Structure {
	protected Random rnd = new Random();

	private static final long MAGIC_X = 341873128712L;
	private static final long MAGIC_Z = 132897987541L;

	private final long structureSeed;
	public final int structurePosRange;
	public final int structureRegionSize;
	protected final HashSet<Integer> validBiomes;

	public Structure(
			long structureSeed, int structurePosRange, int structureRegionSize,
			Biome[] validBiomes) {
		this.structureSeed = structureSeed;
		this.structurePosRange = structurePosRange;
		this.structureRegionSize = structureRegionSize;

		this.validBiomes = new HashSet<Integer>();
		for (Biome biome : validBiomes) {
			this.validBiomes.add(biome.index);
		}
	}

	private void setSeed(long regionX, long regionZ, long worldSeed) {
		rnd.setSeed(regionX * MAGIC_X + regionZ * MAGIC_Z + structureSeed + worldSeed);
	}

	// Ocean monuments and Woodland Mansions average two randInts, other
	// structures just use one.
	protected int getChunkInRegion() {
		return rnd.nextInt(structurePosRange);
	}

	public Location chunkLocationInRegion(long regionX, long regionZ, long worldSeed) {
		setSeed(regionX, regionZ, worldSeed);
		int x = getChunkInRegion();
		int z = getChunkInRegion();
		return new Location(x, z);
	}

	public Location chunkLocationInRegionEdge(
			long regionX, long regionZ, long worldSeed, int edgeSize) {
		setSeed(regionX, regionZ, worldSeed);
		int x = getChunkInRegion();
		if (x >= edgeSize && x < structurePosRange - edgeSize) {
			return null;
		}
		int z = getChunkInRegion();
		if (z >= edgeSize && z < structurePosRange - edgeSize) {
			return null;
		}
		return new Location(x, z);
	}

	public Location fullLocation(int regionX, int regionZ, Location chunk) {
		int x = (regionX * structureRegionSize + chunk.x) * 16 + 8;
		int z = (regionZ * structureRegionSize + chunk.z) * 16 + 8;
		return new Location(x, z);
	}

	public Location fullLocation(int regionX, int regionZ, long worldSeed) {
		Location chunkLocation = chunkLocationInRegion(regionX, regionZ, worldSeed);
		if (chunkLocation == null) {
			return null;
		}
		return fullLocation(regionX, regionZ, chunkLocation);
	}

	private static int fullToRegion(int x, int regionSize) {
		if (x < 0) {
			return (x-8 - regionSize * 16 + 1) / (regionSize * 16);
		}
		return (x-8) / 16 / regionSize;
	}

	public Location fullLocationToRegion(Location location) {
		return new Location(
				fullToRegion(location.x, structureRegionSize),
				fullToRegion(location.z, structureRegionSize));
	}

	public boolean structureWillSpawn(Location location, BiomeGenerator generator) {
		int biomeAt = generator.getBiomeAt(location.x, location.z);
		return validBiomes.contains(biomeAt);
	}

	public boolean structureWillSpawn(int regionX, int regionZ, long worldSeed, BiomeGenerator generator) {
		Location location = fullLocation(regionX, regionZ, worldSeed);
		if (location == null) {
			return false;
		}
		return structureWillSpawn(location, generator);
	}
}
