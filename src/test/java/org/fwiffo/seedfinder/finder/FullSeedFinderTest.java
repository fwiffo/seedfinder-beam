package org.fwiffo.seedfinder.finder;

import java.util.ArrayList;

import org.apache.beam.sdk.values.KV;

import org.junit.experimental.categories.Category;
import org.junit.Test;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.NeedsRunner;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.KV;

import org.fwiffo.seedfinder.Constants;
import org.fwiffo.seedfinder.finder.FullSeedFinder;
import org.fwiffo.seedfinder.types.Location;
import org.fwiffo.seedfinder.types.SeedFamily;
import org.fwiffo.seedfinder.types.SeedMetadata;

@RunWith(JUnit4.class)
public class FullSeedFinderTest {
	@Rule
	public final transient TestPipeline p = TestPipeline.create();

	private final static long[] fullSeeds = new long[]{
		-8833529193886864749L,
		-7842174325911934317L,
		-7550284775062984045L,
		-7520729902508365165L,
		-7491175029953746285L,
		-6610439827826103661L,
		-6379348871946655085L,
		-6289839829352666477L,
		-5354217006766445933L,
		-4675862312893764973L,
		-4503036677193422189L,
		-4201858452113020269L,
		-2513571541802505581L,
		-1662391212229481837L,
		-1361212987149079917L,
		  -28991922377545069L,
		  257831078890613395L,
		 1590333618638858899L,
		 2087699902486588051L,
		 3072299371020462739L,
		 4715831760033983123L,
		 4772689705329535635L,
		 5215168368718686867L,
		 6150791191304907411L,
		 6545700583629957779L,
		 6813101811505080979L,
		 7417147111526148755L,
		 7546344125836339859L,
		 7793479155388295827L,
		 7839359576592132755L,
		 8000081788293917331L,
		 9084323398583364243L,
	};
	private final static Location[] spawns = new Location[]{
		new Location(-180, -184),
		new Location( 132,  256),
		new Location(-136,  132),
		new Location( -32,  252),
		new Location(   0,    0),
		new Location( 152,  252),
		new Location( -76,  248),
		new Location(  -8,  256),
		new Location(-252, -132),
		new Location(-148,  192),
		new Location(  -4,   96),
		new Location(-104,  232),
		new Location(-128,  244),
		new Location(  56,    4),
		new Location(-128,  248),
		new Location(  44,  256),
		new Location(-248,  232),
		new Location( 240,  240),
		new Location(   8,  248),
		new Location( 128,  256),
		new Location( -52,  248),
		new Location(-112,  256),
		new Location( 140,  248),
		new Location(-120,  212),
		new Location(-236,  164),
		new Location( -48,  256),
		new Location( -96,  252),
		new Location(-196,  256),
		new Location(-116,   76),
		new Location(-216,  256),
		new Location(-112,  248),
		new Location( -76,  244),
	};

	private final static Location[] empty = new Location[0];

	@Test
	@Category(NeedsRunner.class)
	public void testHasFullQuadHuts() {
		ArrayList<KV<Long, SeedFamily>> families = new ArrayList<KV<Long, SeedFamily>>();

		Location[] huts1 = new Location[]{
			new Location(-2200, -136), new Location(-2024, -152),
			new Location(-2184,   24), new Location(-2040,   24)};
		families.add(KV.of(223652499L, new SeedFamily(223652499L, huts1)));

		Location[] huts2 = new Location[]{
			new Location(-1160, 1896), new Location(-1016, 1896),
			new Location(-1160, 2072), new Location(-1000, 2056)};
		families.add(KV.of(260844607L, new SeedFamily(260844607L, huts2)));

		ArrayList<KV<Long, SeedMetadata>> expected = new ArrayList<KV<Long, SeedMetadata>>();
		for (int i=0; i<fullSeeds.length; i++) {
			expected.add(KV.of(223652499L,
						new SeedMetadata(fullSeeds[i], spawns[i], huts1, empty, empty)));
		}

		PCollection<KV<Long, SeedMetadata>> results = p
			.apply(Create.of(families))
			.apply(ParDo.of(new FullSeedFinder.VerifyQuadHuts()));

		PAssert.that(results).containsInAnyOrder(expected);

		p.run();
	}
}







