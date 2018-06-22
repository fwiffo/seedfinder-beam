package org.fwiffo.seedfinder.util;

import java.io.*;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

@DefaultCoder(SerializableCoder.class)
public class SeedMetadata implements java.io.Serializable {
	public final long seed;
	private Location[] huts;

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

	public SeedMetadata withFullSeed(long highBits) {
		long fullSeed = (highBits << 48) ^ (seed & 0xffffffffffffL);
		return new SeedMetadata(fullSeed, huts);
	}
}
