# Beam Seedfinder

Minecraft seedfinder written for Apache Beam, so you can throw lots of compute
at it to find comprehensive lists of seeds or seek out very rare seeds.

This is based on work by L64 who developed the fast method of finding quad huts
(and other structures) using just the lower 48 bits of the seed. Watch (his
video)[https://www.youtube.com/watch?v=97OdqeiUfHw] explaining the principle.

The biome-related code in this project come from there. The original can be
found in (egut's github repository)[https://github.com/egut/SciCraftSeedFinder].

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
