package org.fwiffo.seedfinder.structure;

import java.util.Arrays;
import java.util.HashSet;
import org.fwiffo.seedfinder.biome.*;
import org.fwiffo.seedfinder.util.Location;

public class OceanMonument extends SizedStructure {
	private static final int innerSize = 16;
	private final HashSet<Integer> innerBiomes;

	private static final Biome[] WATER_BIOMES = new Biome[] {
		Biome.river,
		Biome.frozenRiver,
		Biome.ocean,
		Biome.frozenOcean,
		Biome.deepOcean,
	};

	public OceanMonument() {
		super(10387313L, 27, 32, WATER_BIOMES, 29);
		this.innerBiomes = new HashSet<Integer>();
		this.innerBiomes.add(Biome.deepOcean.index);
	}

	public boolean structureWillSpawn(Location location, BiomeGenerator generator) {
		if (!areaHasValidBiomes(generator, location, innerSize, innerBiomes)) {;
			return false;
		}
		return super.structureWillSpawn(location, generator);
	}
}
