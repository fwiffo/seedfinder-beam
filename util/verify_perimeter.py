#!/usr/bin/python3
"""Given the far corners of each hut, calculates the ideal AFK position.

Finds the "center" position between four huts - the point that minimizes
the distance to the furthest corner - and gives you the distance. It also
displays a diagram showing the AFKable area around that point.

Most quad-hut configurations will fit fine in a 128 block radius, but some
may have a small AFK area.
"""

import math
import sys


def _distance(a, b):
    # Spawn floors are at y 64, 67 and 70. Witches must drop 32 blocks, so
    # the killing level is at y 32. So the top floor and bottom floor are 38
    # blocks apart. The ideal y level to stand is in the middle, 19 blocks
    # from the top and bottom.
    ax, az = a
    bx, bz = b
    return math.sqrt((ax-bx)**2 + (az-bz)**2 + 19**2)


def _find_center(corners):
    minx = min(x for x, z in corners)
    maxx = max(x for x, z in corners)
    minz = min(z for x, z in corners)
    maxz = max(z for x, z in corners)

    x1 = (minx + maxx) // 2 - 16
    z1 = (minz + maxz) // 2 - 16

    best_distance = 10000
    best_spot = None
    lines = []
    leftx = maxx; rightx = minx
    for z in range(z1, z1+32):
        line = []
        for x in range(x1, x1+32):
            furthest_corner = max(_distance((x, z), c) for c in corners)
            if furthest_corner < best_distance:
                best_distance = furthest_corner
                best_spot = (x, z)
            line.append('+' if furthest_corner < 128 else ' ')
        lines.append(line)

    lines[best_spot[1]-z1][best_spot[0]-x1] = 'X'
    lines = [''.join(line) for line in lines if '+' in line or 'X' in line]
    print()
    print('\n'.join(lines))
    print()
    return best_spot, best_distance


def main(argv):
    if len(argv) != 8:
        print("Usage: verify_perimeter x1 z1 x2 z2 x3 z3 x4 z4")
        sys.exit()

    as_int = tuple(int(x) for x in argv)
    corners = [as_int[0:2], as_int[2:4], as_int[4:6], as_int[6:8]]
    (x, z), max_distance = _find_center(corners)
    print("AFK location: %d, 51, %d\nDistance: %.2f" % (x, z, max_distance))


if __name__ == '__main__':
    main(sys.argv[1:])
