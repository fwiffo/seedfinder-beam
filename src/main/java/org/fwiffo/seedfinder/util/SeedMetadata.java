package org.fwiffo.seedfinder.util;

import java.io.*;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;

@DefaultCoder(AvroCoder.class)
public class SeedMetadata implements java.io.Serializable {
	public final long seed;
	private Location[] huts;

	public SeedMetadata() {seed=0;}

	public SeedMetadata(long seed) {
		this.seed = seed;
	}

	public SeedMetadata(long seed, Location[] huts) {
		this.seed = seed;
		this.huts = huts;
	}

	public Location[] getHuts() {
		return huts;
	}

	public void setHuts(Location[] huts) {
		this.huts = huts;
	}

	public SeedMetadata withFullSeed(long fullSeed) {
		return new SeedMetadata(fullSeed, huts);
	}

	public String asString() {
		return String.format("%20d %s %s %s %s", seed,
				huts[0].asString(), huts[1].asString(), huts[2].asString(), huts[3].asString());
	}
}
