package org.fwiffo.seedfinder.util;

import java.io.Serializable;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;

import org.fwiffo.seedfinder.util.SeedMetadata;

@DefaultCoder(AvroCoder.class)
public class SeedFamily implements Serializable {
	public final long baseSeed;
	public final long[] fullSeeds;
	public final Location[] huts;
	public final Location[] mansions;
	public final Location[] monuments;

	public SeedFamily() {
		this.baseSeed = 0;
		this.fullSeeds = new long[0];
		this.huts = new Location[0];
		this.mansions = new Location[0];
		this.monuments = new Location[0];
	}

	public SeedFamily(
			long baseSeed, long fullSeeds[],
			Location[] huts, Location[] mansions, Location[] monuments) {
		this.baseSeed = baseSeed;
		this.fullSeeds = fullSeeds;
		this.huts = huts;
		this.mansions = mansions;
		this.monuments = monuments;
	}

	public SeedFamily(
			long baseSeed,
			Location[] huts, Location[] mansions, Location[] monuments) {
		this.baseSeed = baseSeed;
		this.fullSeeds = new long[0];
		this.huts = huts;
		this.mansions = mansions;
		this.monuments = monuments;
	}

	public SeedFamily(long baseSeed, Location[] huts) {
		this.baseSeed = baseSeed;
		this.fullSeeds = new long[0];
		this.huts = huts;
		this.mansions = new Location[0];
		this.monuments = new Location[0];
	}

	public SeedFamily withMansions(Location[] mansions) {
		return new SeedFamily(baseSeed, huts, mansions, monuments);
	}

	public SeedFamily withMonuments(Location[] monuments) {
		return new SeedFamily(baseSeed, huts, mansions, monuments);
	}

	public SeedMetadata expanded(long fullSeed, Location spawn) {
		return new SeedMetadata(fullSeed, spawn, huts, mansions, monuments);
	}
}

