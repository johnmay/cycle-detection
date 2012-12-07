package org.openscience.cdk.ringsearch;

import org.openscience.cdk.interfaces.IAtom;

/**
 * Describes an algorithm capable of testing whether an atom is in a ring
 *
 * @author John May
 */
public interface RingTester {

    public boolean isInRing(int i);

    public boolean isInRing(IAtom atom);

}
