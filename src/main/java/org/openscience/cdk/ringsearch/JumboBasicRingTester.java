package org.openscience.cdk.ringsearch;

import org.openscience.cdk.interfaces.IAtom;

import java.util.BitSet;
import java.util.List;

class JumboBasicRingTester implements RingTester {

    private final List<List<Integer>> graph;
    private final int n;
    private final BitSet visited;
    private final BitSet rings;

    // search stack
    private final BitSet[] stack;
    private final BitSet EMPTY;

    protected JumboBasicRingTester(List<List<Integer>> graph) {
        this.graph = graph;
        this.n = graph.size();
        this.visited = new BitSet(n);
        this.rings = new BitSet(n);

        this.stack = new BitSet[n];
        this.EMPTY = new BitSet(n);

        // check from all unvisited vertices
        for (int i = 0; i < n; i++) {
            if (!visited(i)) check(i, EMPTY, copy(EMPTY));
        }


    }

    @Override
    public boolean isInRing(int i) {
        return rings.get(i);
    }

    @Override
    public boolean isInRing(IAtom atom) {
        throw new IllegalStateException("this ring tester does not store containers/atoms");
    }

    public boolean visited(int i) {
        return visited.get(i);
    }

    /**
     * @return visited vertexes
     */
    public void check(int i, BitSet parentPath, BitSet path) {

        stack[i] = copy(path);
        path.set(i);
        visited.set(i);

        for (int j : graph.get(i)) {
            if (visited(j)) {
                if (parentPath.get(j)) {
                    registerCycle(xor(stack[j], path));
                }
            } else {
                check(j, stack[i], copy(path));
            }
        }
    }

    public void registerCycle(BitSet cycle) {
        rings.or(cycle);
    }

    private static BitSet xor(BitSet set1, BitSet set2) {
        BitSet result = copy(set1);
        result.xor(set2);
        return result;
    }

    private static BitSet copy(BitSet bs) {
        return (BitSet) bs.clone();
    }

    private static String toString(BitSet bs) {
        StringBuilder sb = new StringBuilder(bs.size());
        sb.append("{");
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            sb.append(i + 1);
            if (bs.nextSetBit(i + 1) > 0)
                sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }
}
