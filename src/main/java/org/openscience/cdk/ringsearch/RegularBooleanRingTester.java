package org.openscience.cdk.ringsearch;

import org.openscience.cdk.interfaces.IAtom;

import java.util.List;

/**
 * @author John May
 */
class RegularBooleanRingTester implements RingTester {

    private final List<List<Integer>> graph;
    private final int n;
    private long explored;
    private long rings;

    // search stack
    private final long[] stack;

    protected RegularBooleanRingTester(List<List<Integer>> graph) {

        this.graph = graph;
        this.n = graph.size();

        this.stack = new long[n];
        this.explored = 0;
        this.rings = 0;

    }

    private static boolean isBitSet(long value, int bit) {
        return (value & (1L << bit)) != 0;
    }

    private static long setBit(long value, int bit) {
        return value | (1L << bit);
    }

    @Override
    public boolean isInRing(IAtom atom) {
        throw new IllegalStateException("this ring tester does not store containers/atoms");
    }

    @Override
    public boolean isInRing(int i) {
        if (!isBitSet(explored, i))
            check(i, 0, 0);
        return isBitSet(rings, i);
    }

    /**
     * Do a complete DFS from <i>vertex</i> i.
     *
     * @param i
     * @param parentPath
     * @param path
     */
    private void check(int i, long parentPath, long path) {

        explored |= path = setBit(stack[i] = path, i);

        for (int j : graph.get(i)) {
            if (isBitSet(parentPath, j))
                rings |= stack[j] ^ path;
            else if (!isBitSet(explored, j))
                check(j, stack[i], path);
        }

    }

}
