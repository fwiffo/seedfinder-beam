package org.fwiffo.seedfinder;

import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
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
	}

	public static void main(String[] args) {
		SeedFinderOptions options =
			PipelineOptionsFactory.fromArgs(args).withValidation().as(SeedFinderOptions.class);
		Pipeline p = Pipeline.create(options);

		// TODO: Generate 1 -> 2**48-1, pipe it through filters for potential
		// structure generation, expand to full 64-bits, pipe it through filters
		// for actual structure generation and biomes and whatever else.
		p.apply(GenerateSequence.from(1L).to(
					4000000000L/StructureFinder.PotentialQuadHutFinder.BATCH_SIZE))
		.apply(ParDo.of(new StructureFinder.PotentialQuadHutFinder()))
		.apply(ParDo.of(new StructureFinder.QuadHutVerifier()))
		.apply(ParDo.of(new BiomeFinder.HasMostlyOceanSpawn()))
		.apply(ParDo.of(new DoFn<KV<Long, SeedMetadata>, String>() {
			@ProcessElement
			public void processElement(ProcessContext c)  {
				c.output(c.element().getValue().asString());
			}
		}))
		.apply("WriteSeeds", TextIO.write().to(options.getOutput()));

		p.run().waitUntilFinish();
	}
}
