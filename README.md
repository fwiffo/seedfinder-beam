# Beam Seedfinder

Minecraft seedfinder written for Apache Beam, so you can throw lots of compute
at it to find comprehensive lists of seeds or seek out very rare seeds.

This is based on work by L64 who developed the fast method of finding quad huts
(and other structures) using just the lower 48 bits of the seed. Watch (his
video)[https://www.youtube.com/watch?v=97OdqeiUfHw] explaining the principle.

The biome-related code in this project came from there. The original can be
found in (egut's github repository)[https://github.com/egut/SciCraftSeedFinder].

## Verifying the output

The vast majority of the seeds produced by this program will be good quad witch
hut configurations, but please verify all structures in creative mode or
something before committing to a survival seed for your world!

In a few rare cases, a structure might not generate, because it doesn't take
into account every detail (in particular, it uses quarter-resolution biome data
for performance). Essentially, it's got the same limitations as Amidst.

It also does not know the orientation of structures (if you know how Minecraft
picks this, please let me know). The computed coordinates are not at the center
of the huts, so it's not possible to compute the bounding box (or even
reasonably approximate). As such it can't verify that there is a place within
the perimeter that is in range of all the spawning spaces. Most seeds will be
fine though.

## Usage

```
  --all_biomes_nearby=<boolean>
    Default: false
    Search for seeds with all biome types within the search radius.
  --bulk_search_mode=<boolean>
    Default: false
    Bulk search mode ignores search options other than seed range, search radius
    and timeout. Outputs a binary format of all quad huts seed for further
    searching later.
  --end_seed=<String>
    Default: 1G
    Lower 48 bits of end seed for search; 0 to 256T. suffixes of K, M, G and T
    may be used.
  --max_sequence_time=<int>
    Default: 0
    Maximum time for generating candidate seeds, in minutes.
  --ocean_monument_near_huts=<int>
    Default: 0
    Search for seeds with ocean monuments close to quad huts; a max distance
    from the perimeter in chunks. 5 is rare, and pretty close, 4 or less is
    vanishingly rare. Monuments inside the perimeter are theoretically possible,
    but might be too rare to actually exist.
  --output=<String>
    Path of the file to write to.
  --search_radius=<int>
    Default: 2048
    Radius to search for structures and biomes, in blocks; structures will round
    up to an integer number of regions.
  --spawn_biomes=<none | flower_forest | ice_spikes | jungle | mega_taiga | mesa | mushroom_island | ocean>
    Default: none
    Search for seeds with specific biomes at spawn.
  --start_seed=<String>
    Default: 0
    Lower 48 bits of start seed for search; 0 to 256T. suffixes of K, M, G and T
    may be used.
  --woodland_mansions=<int>
    Default: 0
    Search for seeds with a number nearby woodland mansions.
```

Be aware that seeds matching some of these criteria are rare, and some
combinations will take days to find, if they exist at all.

### Running locally

This is written with the intention of being run in a massively parallel
environment in the cloud, but Beam does a quite good job of parallelizing
the pipeline and running it locally.

This is a maven project. To run:

```sh
mvn compile exec:java -Dexec.args="
    --output=./output/seeds \
    --start_seed=0 --end_seed=1G \
    ... (other options below) ..."
```

### Running with Google Dataflow

Fancy! Get your project/user credentials from your Google Cloud Console.

```sh
GOOGLE_APPLICATION_CREDENTIALS='credentials.json' mvn compile exec:java \
    -Dexec.args="
    --project=[your cloud dataflow project] \
    --stagingLocation=[your cloud storage staging bucket] \
    --region=us-west1 (or whatever your favorite region is) \
    --runner=DataflowRunner \
    --jobName=minecraft-seed-finder \
    --output=[your cloud storage output bucket] \
    --start_seed=0 --end_seed=4G \
    ... (other options below) ..."
```

### Bulk mode

There is a "bulk" mode. This outputs families of seeds grouped by their lower
48-bits in a binary Avro format.

If you want to do more than one kind of search, it's a good idea to create a
list of saved "generic" quad witch hut seeds, then store those for later
filtering. This will save you from repeating the expensive work of eliminating
enormous numbers of non-quad seeds, and verifying that all four huts will spawn.

This mode ignores search parameters other than seed range, radius and timeout.

You can load it later with the --input flag (still to be added).

## Note on Minecraft versions

Witch hut (and other structure) generation changed in the 1.13 snapshot 18w06a.
There is a "magic" seed for seeding the RNG for structure generation; in
previous versions it was 14357617 (and remains that for desert temples), but was
changed to 14357620 for witch huts for 1.13.

A new bug in 1.13, MC-131462, prevents some structures from generated in
negative coordinates. In the case of quad witch huts, some of the huts won't
generate unless the south-east hut has non-negative coordinates for both X and
Z. This makes quad seeds four times as rare, but the seedfinder isn't slower
because it can just search the positive coordinates.

If this bug is fixed, I will modify the pipeline to include negative
coordinates.

## TODOS:

 - Search for lots of a particular biome near spawn, e.g. lots of mushroom
   islands.
 - Search for strongholds near quad witch huts.
 - Improve performance.
 - Unit tests.
 - Javadocs.
