package org.fwiffo.seedfinder.structure;

import org.fwiffo.seedfinder.biome.Biome;

public class WoodlandMansion extends SizedStructure {
	private static final Biome[] ROOFED_FOREST_BIOMES = new Biome[] {
		Biome.roofedForest,
		Biome.roofedForestM,
	};

	public WoodlandMansion() {
		super(10387319L, 60, 80, ROOFED_FOREST_BIOMES, 32);
	}
}
