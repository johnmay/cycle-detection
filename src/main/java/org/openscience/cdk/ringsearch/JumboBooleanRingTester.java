package org.openscience.cdk.ringsearch;

import org.openscience.cdk.interfaces.IAtom;

import java.util.BitSet;
import java.util.List;

class JumboBooleanRingTester implements RingTester {

    private final List<List<Integer>> graph;
    private final int n;
    private final BitSet explored;
    private final BitSet rings;

    // search stack
    private final BitSet[] stack;
    private final BitSet EMPTY;

    protected JumboBooleanRingTester(List<List<Integer>> graph) {
        this.graph = graph;
        this.n     = graph.size();
        this.explored = new BitSet(n);
        this.rings = new BitSet(n);

        this.stack = new BitSet[n];
        this.EMPTY = new BitSet(n);
    }

    @Override
    public boolean isInRing(int i) {

        if (explored.get(i)) {
            return rings.get(i);
        }

        // haven't explored the that node (new search or disconnected molecule)
        check(i, i, rings, stack, copy(EMPTY));

        return rings.get(i);
    }

    @Override
    public boolean isInRing(IAtom atom) {
        throw new IllegalStateException("this ring tester does not store containers/atoms");
    }

    /**
     * @return explored vertexes
     */
    public void check(int i, int parent, BitSet rings, BitSet[] stack, BitSet path) {

        BitSet state = copy(path);
        stack[i] = state;

        path.set(i);
        explored.set(i);

        for (int j : graph.get(i)) {
            if (j != parent) {
                if (path.get(j)) {
                    rings.or(xor(stack[j], path));
                } else if (!explored.get(j)) {
                    check(j, i, rings, stack, copy(path));
                }
            }
        }
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
