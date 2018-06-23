package org.fwiffo.seedfinder;

import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fwiffo.seedfinder.finder.StructureFinder;
import org.fwiffo.seedfinder.finder.BiomeFinder;
import org.fwiffo.seedfinder.util.SeedMetadata;

/**
 * Minecraft seed finder Beam pipeline.
 *
 * <p>To run locally
 * mvn compile exec:java \
 *   -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
 *   -Dexec.args="--output=./output/"
 *
 * <p>To run using managed resource in Google Cloud Platform:
 * mvn compile exec:java \
 *   -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
 *   -Dexec.args="--project=<YOUR_PROJECT_ID>"
 *   --stagingLocation=<STAGING_LOCATION_IN_CLOUD_STORAGE>
 *   --output=<OUTPUT_LOCATION_IN_CLOUD_STORAGE>
 *   --runner=DataflowRunner
 */
public class SeedFinderPipeline {
	private static final Logger LOG = LoggerFactory.getLogger(SeedFinderPipeline.class);
	// Only search the lower 48 bits of the key space.
	private static final long MAX_SEED = (1<<48) - 1;

	public interface SeedFinderOptions extends PipelineOptions {
		@Description("Path of the file to write to")
		@Required
		String getOutput();
		void setOutput(String value);

		@Description("Radius to search for structures and biomes, in blocks; " +
				"structures will round up to an integer number of regions")
		@Default.Integer(2048)
		int getSearch_radius();
		void setSearch_radius(int value);

		@Description("Look for seeds with spawn chunks that are mostly ocean")
		@Default.Boolean(false)
		boolean getOcean_spawn();
		void setOcean_spawn(boolean value);

		@Description("Search for seeds with a number nearby Woodland Mansions.")
		@Default.Integer(0)
		int getWoodland_mansions();
		void setWoodland_mansions(int value);

		@Description("Start seed for search - lower 48-bits only")
		@Default.Long(0)
		long getStart_seed();
		void setStart_seed(long value);

		@Description("End seed for search - lower 48-bits only")
		@Default.Long(1<<30)
		long getEnd_seed();
		void setEnd_seed(long value);
	}

	public static void main(String[] args) {
		SeedFinderOptions options =
			PipelineOptionsFactory.fromArgs(args).withValidation().as(SeedFinderOptions.class);
		Pipeline p = Pipeline.create(options);

		// Operations on the lower 48-bits of the seed.
		PCollection<KV<Long, SeedMetadata>> seeds = p
			.apply(GenerateSequence.from(
					options.getStart_seed() / StructureFinder.HasPotentialQuadHuts.BATCH_SIZE).to(
					options.getEnd_seed() / StructureFinder.HasPotentialQuadHuts.BATCH_SIZE))
			.apply(ParDo.of(new StructureFinder.HasPotentialQuadHuts(options.getSearch_radius())));

		if (options.getWoodland_mansions() > 0) {
			seeds = seeds.apply(ParDo.of(new StructureFinder.FindPotentialWoodlandMansions()));
		}

		// Expands to full 64-bit seeds to do checks that require biomes.
		seeds = seeds.apply(ParDo.of(new StructureFinder.VerifyQuadHuts()));

		if (options.getWoodland_mansions() > 0) {
			seeds = seeds.apply(ParDo.of(
					new StructureFinder.VerifyWoodlandMansions(options.getWoodland_mansions())));
		}

		if (options.getOcean_spawn()) {
			seeds = seeds.apply(ParDo.of(new BiomeFinder.HasMostlyOceanSpawn()));
		}

		seeds.apply(ParDo.of(new DoFn<KV<Long, SeedMetadata>, String>() {
			@ProcessElement
			public void processElement(ProcessContext c)  {
				c.output(c.element().getValue().asString());
			}
		}))
		.apply("WriteSeeds", TextIO.write().to(options.getOutput()));

		p.run().waitUntilFinish();
	}
}
