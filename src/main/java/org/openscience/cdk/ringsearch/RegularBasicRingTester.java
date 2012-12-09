package org.openscience.cdk.ringsearch;

import org.openscience.cdk.interfaces.IAtom;

import java.util.List;

/**
 * @author John May
 */
class RegularBasicRingTester implements RingTester {

    private final List<List<Integer>> graph;
    private final int n;
    private long visited;
    private long cyclic;

    // search state
    private final long[] state;

    protected RegularBasicRingTester(List<List<Integer>> graph) {

        this.graph = graph;
        this.n = graph.size();

        this.state = new long[n];
        this.visited = 0;
        this.cyclic = 0;

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
        if (!isBitSet(visited, i))
            check(i, 0, 0);
        return isBitSet(cyclic, i);
    }

    public boolean visited(int i){
        return isBitSet(visited, i);
    }

    /**
     * Do a complete DFS from <i>vertex</i> i.
     *
     * @param i
     * @param parentPath
     * @param path
     */
    private void check(int i, long parentPath, long path) {

        visited |= path = setBit(state[i] = path, i);

        for (int j : graph.get(i)) {
            if (visited(j)) {
                if (isBitSet(parentPath, j))
                    cyclic |= state[j] ^ path;
                else
                    check(j, state[i], path);
            }
        }

    }

}
