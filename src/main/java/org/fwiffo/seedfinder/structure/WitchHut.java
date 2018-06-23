package org.fwiffo.seedfinder.structure;

import org.fwiffo.seedfinder.biome.Biome;

public class WitchHut extends Structure {
	private static final Biome[] SWAMP_BIOMES = new Biome[] {
		Biome.swampland,
	};

	public WitchHut() {
		super(14357620L, 24, 32, SWAMP_BIOMES);
	}
}
