package org.fwiffo.seedfinder.types;

import java.io.*;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;

@DefaultCoder(AvroCoder.class)
public class Location implements java.io.Serializable {
	public final int x;
	public final int z;

	public Location() {x=0; z=0;}

	public Location(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public String asString() {
		return String.format("(%5d, %5d)", x, z);
	}
}
