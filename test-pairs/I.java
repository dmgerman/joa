package ca.juliusdavies.signature.testgarbage;

import ca.juliusdavies.signature.Artifact;

import java.io.Serializable;
import java.util.*;

public interface I <D extends Artifact> extends Serializable {

    public final static int CONSTANT_GARDENER = 55;

    final static double CONSTANT_DOUBLE = 55.0;

    static Comparator<Map<Set<String[][]>,Integer>> comparator = new Comparator<Map<Set<String[][]>, Integer>>() {
        public int compare(Map<Set<String[][]>, Integer> o1, Map<Set<String[][]>, Integer> o2) {
            return 5;
        }
    };

    final List<Artifact> list = new ArrayList<Artifact>();

    int i = 5;


    public enum InterfaceEnum {
        TOES, FINGERS;

        private InterfaceEnum() {}

        private InterfaceEnum(String s,int i) {}

        InterfaceEnum(long l) {}

        InterfaceEnum(long[] l) {}

        InterfaceEnum(long[][] l) {}
    }


    public int getSix();
}
