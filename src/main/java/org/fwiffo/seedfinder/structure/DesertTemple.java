package org.fwiffo.seedfinder.structure;

import org.fwiffo.seedfinder.biome.Biome;

public class DesertTemple extends Structure {
	private static final Biome[] DESERT_BIOMES = new Biome[] {
		Biome.desert,
		Biome.desertHills,
	};

	public DesertTemple() {
		super(14357617L, 24, 32, DESERT_BIOMES);
	}
}
