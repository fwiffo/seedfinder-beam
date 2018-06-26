#!/usr/bin/python3
import sys

VCPU_HR = 0.056
MEM_GB_HR = 0.003557
PD_GB_HR = 0.000054
SSD_GB_HR = 0.000298
DATA_GB = 0.011


def cost_estimate(vcpu, memory, storage, ssd=0, data=0):
    return (VCPU_HR * vcpu +
            MEM_GB_HR * memory +
            PD_GB_HR * storage +
            SSD_GB_HR * ssd +
            DATA_GB * data)


def main(argv):
    print("$%.2f" % cost_estimate(*(float(a) for a in argv)))


if __name__ == '__main__':
    main(sys.argv[1:])
