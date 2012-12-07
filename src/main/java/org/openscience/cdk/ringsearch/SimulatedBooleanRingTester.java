package org.openscience.cdk.ringsearch;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author John May
 */
public class SimulatedBooleanRingTester implements RingTester {

    private RingTester tester;
    private IAtomContainer container;

    public SimulatedBooleanRingTester(IAtomContainer container) {
        this.container = container;
        if (container.getAtomCount() < 64)
            tester = new RegularBooleanRingTester(createList(container));
        else
            tester = new JumboBooleanRingTester(createList(container));
    }

    @Override
    public boolean isInRing(int i) {
        return false; // simulated
    }

    @Override
    public boolean isInRing(IAtom atom) {
        return tester.isInRing(container.getAtomNumber(atom));
    }

    public static int[][] create(IAtomContainer container) {

        int n = container.getAtomCount();
        int[][] graph = new int[n][12];
        int[] connected = new int[n];

        // ct table only
        for (IBond bond : container.bonds()) {
            int a1 = container.getAtomNumber(bond.getAtom(0));
            int a2 = container.getAtomNumber(bond.getAtom(1));
            graph[a1][connected[a1]++] = a2;
            graph[a2][connected[a2]++] = a1;
        }

        // trim the neighbours
        for (int i = 0; i < n; i++) {
            graph[i] = Arrays.copyOf(graph[i], connected[i]);
        }

        return graph;
    }

    public static List<List<Integer>> createList(IAtomContainer container) {

        int n = container.getAtomCount();

        List<List<Integer>> graph = new ArrayList<List<Integer>>(n);

        for (int i = 0; i < n; i++)
            graph.add(new ArrayList<Integer>(6));

        // ct table only
        for (IBond bond : container.bonds()) {
            int i = container.getAtomNumber(bond.getAtom(0));
            int j = container.getAtomNumber(bond.getAtom(1));
            graph.get(i).add(j);
            graph.get(j).add(i);
        }

        return graph;

    }

}
