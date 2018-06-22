package org.fwiffo.seedfinder.structure;

import java.util.Arrays;
import java.util.HashSet;
import org.fwiffo.seedfinder.util.Location;

public class OceanMonument extends SizedStructure {
	private static final int innerSize = 16;
	private final HashSet<Integer> innerBiomes;

	public OceanMonument() {
		// Ocean, Frozen Ocean, River, Frozen River, Deep Ocean
		super(10387313L, 27, 32, new Integer[]{0, 10, 7, 11, 24}, 29);
		this.innerBiomes = new HashSet<Integer>();
		this.innerBiomes.add(24);
	}

	/*
	public boolean structureWillSpawn(Location location, BiomeGenerator generator) {
		if (!areaHasValidBiomes(generator, location, innerSize, innerBiomes)) {;
			return false;
		}
		return super.structureWillSpawn(location, generator);
	}
	*/
}
