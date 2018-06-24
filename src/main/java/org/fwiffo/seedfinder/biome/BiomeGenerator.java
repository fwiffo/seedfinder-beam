package org.fwiffo.seedfinder.biome;

import java.util.*;
import minecraft.layer.*;

public class BiomeGenerator {
	private GenLayer biomeIndexLayer;
	private GenLayer biomeIndexLayerQuarter;

	public BiomeGenerator(long seed) {
		setSeed(seed);
	}

	public void setSeed(long seed) {
		// TODO: Maybe we can increase performance by only creating one
		// generator with threadLocals and only resetting the cache if
		// the seed changes? I don't understand the biome internals.
		IntCache.resetIntCache();
		biomeIndexLayer = GenLayer.func_180781_a(seed, null)[1];
		biomeIndexLayerQuarter = GenLayer.func_180781_a(seed, "")[0]; // 1:4 fourth resolution less calculations
	}

	public int getBiomeAt(int x, int z) {
		return biomeIndexLayer.getInts(x, z, 1, 1)[0];
	}

	public int getBiomeAtCenterOfChunk(int x, int z) {
		return getBiomeAt(x*16 + 8, z*16 + 8);
	}

	public int[] getFullResolutionBiomeData(int x, int y, int width, int height) {
		return biomeIndexLayer.getInts(x, y, width, height);
	}

	public int[] getQuarterResolutionBiomeData(int x, int y, int width, int height) {
		return biomeIndexLayerQuarter.getInts(x, y, width, height);
	}

	public Hashtable<Integer, Float> biomeCensus(int x, int z, int radius) {
		int left = (x - radius) / 4;
		int top = (z - radius) / 4;
		// * 2 to convert to width, / 4 for quarter resolution.
		int size = radius / 2;

		int[] biomes = getQuarterResolutionBiomeData(left, top, size, size);
		Hashtable<Integer, Float> census = new Hashtable<Integer, Float>();
		int area = size*size;
		for (int i=0; i<256; i++) {
			census.put(i, 0f);
		}
		for (int i=0; i<area; i++) {
			census.put(biomes[i], census.get(biomes[i]) + 1);
		}
		for (Integer i : census.keySet()) {
			census.put(i, census.get(i) / area);
		}
		return census;
	}
}
