package org.fwiffo.seedfinder.structure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import org.fwiffo.seedfinder.util.Location;

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
			Integer[] validBiomes) {
		this.structureSeed = structureSeed;
		this.structurePosRange = structurePosRange;
		this.structureRegionSize = structureRegionSize;
		this.validBiomes = new HashSet<Integer>(Arrays.asList(validBiomes));
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
		if (x < 2 && regionX < 0) { // Emulating 1.13 bug MC-131462.
			return null;
		}
		int z = getChunkInRegion();
		if (z < 2 && regionZ < 0) { // Emulating 1.13 bug MC-131462.
			return null;
		}
		return new Location(x, z);
	}

	public Location chunkLocationInRegionEdge(
			long regionX, long regionZ, long worldSeed, int edgeSize) {
		setSeed(regionX, regionZ, worldSeed);
		int x = getChunkInRegion();
		if (x < 2 && regionX < 0) { // Emulating 1.13 bug MC-131462.
			return null;
		}
		if (x >= edgeSize && x < structurePosRange - edgeSize) {
			return null;
		}
		int z = getChunkInRegion();
		if (z < 2 && regionZ < 0) { // Emulating 1.13 bug MC-131462.
			return null;
		}
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

	/*
	public boolean structureWillSpawn(XZPair location, BiomeGenerator generator) {
		int biomeAt = generator.getBiomeAt(location.getX(), location.getZ());
		return validBiomes.contains(biomeAt);
	}

	public boolean structureWillSpawn(int regionX, int regionZ, long worldSeed, BiomeGenerator generator) {
		XZPair location = locationForRegion(regionX, regionZ, worldSeed);
		if (location == null) {
			return false;
		}
		return structureWillSpawn(location, generator);
	}
	*/
}
