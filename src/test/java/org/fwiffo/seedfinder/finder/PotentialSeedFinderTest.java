package org.fwiffo.seedfinder.finder;

import java.util.ArrayList;

import org.apache.beam.sdk.values.KV;

import org.junit.experimental.categories.Category;
import org.junit.Test;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.NeedsRunner;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.KV;

import org.fwiffo.seedfinder.Constants;
import org.fwiffo.seedfinder.finder.PotentialSeedFinder;
import org.fwiffo.seedfinder.types.SeedFamily;
import org.fwiffo.seedfinder.types.Location;

@RunWith(JUnit4.class)
public class PotentialSeedFinderTest {
	@Rule
	public final transient TestPipeline p = TestPipeline.create();

	public final long startSeed = 200*1024*1024;
	public final long endSeed = 256*1024*1024;
	public final int radius = 2048;

	@Test
	@Category(NeedsRunner.class)
	public void testHasPotentialQuadHuts() {
		ArrayList<KV<Long, SeedFamily>> expected = new ArrayList<KV<Long, SeedFamily>>();

		Location[] huts1 = new Location[]{
			new Location(1400, -1176), new Location(1544, -1176),
			new Location(1400, -1000), new Location(1560, -1016)};
		expected.add(KV.of(262179349L, new SeedFamily(262179349L, huts1)));

		Location[] huts2 = new Location[]{
			new Location(-2200, -136), new Location(-2024, -152),
			new Location(-2184,   24), new Location(-2040,   24)};
		expected.add(KV.of(223652499L, new SeedFamily(223652499L, huts2)));

		Location[] huts3 = new Location[]{
			new Location(-1160, 1896), new Location(-1016, 1896),
			new Location(-1160, 2072), new Location(-1000, 2056)};
		expected.add(KV.of(260844607L, new SeedFamily(260844607L, huts3)));

		PCollection<KV<Long, SeedFamily>> results = p
			.apply(GenerateSequence
					.from(startSeed / Constants.BATCH_SIZE).to(endSeed / Constants.BATCH_SIZE))
			.apply(ParDo.of(new PotentialSeedFinder.HasPotentialQuadHuts(radius)));

		PAssert.that(results).containsInAnyOrder(expected);

		p.run();
	}
}
