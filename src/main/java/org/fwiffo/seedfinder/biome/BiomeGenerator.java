package org.fwiffo.seedfinder.biome;

import java.util.*;
import minecraft.layer.*;

public class BiomeGenerator {
	public final GenLayer biomeIndexLayer;
	public final GenLayer biomeIndexLayerquarter;
	public BiomeGenerator(long seed, int quarter) {
		IntCache.resetIntCache();
		if(quarter == 0) {
			biomeIndexLayer = GenLayer.func_180781_a(seed, "")[1]; //1:1 resolution
			biomeIndexLayerquarter = null;
		} else if(quarter == 1) {
			biomeIndexLayer = null;
			biomeIndexLayerquarter = GenLayer.func_180781_a(seed, "")[0]; // 1:4 fourth resolution less calculations
		} else {
			biomeIndexLayer = GenLayer.func_180781_a(seed, null)[1];
			biomeIndexLayerquarter = GenLayer.func_180781_a(seed, "")[0]; // 1:4 fourth resolution less calculations
		}

	}

	public BiomeGenerator(long seed) {
		this(seed, 2);
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
		return biomeIndexLayerquarter.getInts(x, y, width, height);
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
