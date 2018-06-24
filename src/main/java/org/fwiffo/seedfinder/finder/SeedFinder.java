package org.fwiffo.seedfinder.finder;

import java.lang.ThreadLocal;

import org.fwiffo.seedfinder.structure.OceanMonument;
import org.fwiffo.seedfinder.structure.WitchHut;
import org.fwiffo.seedfinder.structure.WoodlandMansion;

public class SeedFinder {
	protected static final ThreadLocal<WitchHut> threadWitchHut =
			ThreadLocal.withInitial(() -> new WitchHut());
	protected static final ThreadLocal<OceanMonument> threadOceanMonument =
			ThreadLocal.withInitial(() -> new OceanMonument());
	protected static final ThreadLocal<WoodlandMansion> threadWoodlandMansion =
			ThreadLocal.withInitial(() -> new WoodlandMansion());
}
