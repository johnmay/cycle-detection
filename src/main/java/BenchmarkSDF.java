import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.SpanningTree;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.isomorphism.matchers.IQueryAtomContainer;
import org.openscience.cdk.ringsearch.BasicRingTester;
import org.openscience.cdk.ringsearch.RingTester;
import org.openscience.cdk.ringsearch.SimulatedBooleanRingTester;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author John May
 */
public class BenchmarkSDF {

    private static final CommandLineParser parser = new BasicParser();
    private static final Options options = new Options();

    static {
        options.addOption(new Option("i", "sdf", true, "SDF file"));
        options.addOption(new Option("r", "reps", true, "number of repetitions to test (default = 50)"));
        options.addOption(new Option("f", "filter", true, "filter - only test molecules below this size"));
        options.addOption(new Option("x", "stress-test", false, "perform a stress test with a very large molecule"));
        options.addOption(new Option("s", "simulate", false, "simulates the BasicRingTester - i.e. calculate the overhead of just converting to an adjacency list"));
        options.addOption(new Option("h", "help", false, "print help"));
    }

    private static int reps = 50;
    private static int filter = Integer.MAX_VALUE;
    private static boolean stressTest = false;
    private static boolean simulate = false;

    public static void main(String[] args) throws ParseException {

        CommandLine cli = parser.parse(options, args);

        if (cli.hasOption("h"))
            help();

        String sdf = cli.getOptionValue("i");

        if (sdf == null || sdf.isEmpty()) {
            System.err.println("input '-i' is required");
            help();
        }

        if (cli.hasOption("x"))
            stressTest = true;

        if (cli.hasOption("s"))
            simulate = true;

        if (cli.hasOption("f"))
            filter = Integer.parseInt(cli.getOptionValue("f"));

        if (cli.hasOption("r"))
            reps = Integer.parseInt(cli.getOptionValue("r"));

        List<IAtomContainer> molecules;
        if (stressTest) {
            System.out.println("[BENCHMARK] loading stress test");
            molecules = loadExtremeMol();
        } else {
            System.out.printf("[BENCHMARK] loading %s", sdf);
            if (filter != Integer.MAX_VALUE)
                System.out.printf(" [filter=%d]", filter);
            System.out.print("...");
            molecules = load(sdf);
            System.out.println("done");
        }


        System.out.printf("[BENCHMARK] loaded %d molecules\n", molecules.size());

        System.out.printf("[BENCHMARK] checking counts and warming up\n");

        RingTestBenchmark bitwiseBM = simulate ? new SimulatedBooleanRingTesterBenchmark() : new BooleanRingTesterBenchmark();
        RingTestBenchmark treeBM = new SpanningTreeBenchMark();

        DescriptiveStatistics dummy = new DescriptiveStatistics(); // don't measure correctness

        int bitwiseCount = bitwiseBM.benchmark(molecules, dummy);
        int treeCount = treeBM.benchmark(molecules, dummy);

        System.out.printf("[BENCHMARK] BasicRingTester indicated there were %d atoms in rings\n", bitwiseCount);
        System.out.printf("[BENCHMARK] SpanningTree      indicated there were %d atoms in rings\n", treeCount);

        if (!stressTest) {
            System.out.print("[BENCHMARK] warming up");
            for (int i = 0; i < 10; i++) {
                bitwiseBM.benchmark(molecules, dummy);
                treeBM.benchmark(molecules, dummy);
                System.out.print(".");
            }
            System.out.println("done");
        } else {
            System.out.println("[BENCHMARK] skipping warp up on stress test");
        }

        DescriptiveStatistics bitwiseStats = new DescriptiveStatistics();
        DescriptiveStatistics treeStats = new DescriptiveStatistics();

        if (stressTest)
            reps = 5;  // 50 is way to many for the stress test

        System.out.printf("[BENCHMARK] Starting benchmark, %d repetitions\n", reps);
        System.out.print("[BENCHMARK] Testing BasicRingTester");
        for (int r = 0; r < reps; r++) {
            System.out.print(".");
            bitwiseBM.benchmark(molecules, bitwiseStats);
        }
        System.out.println("done");

        System.out.print("[BENCHMARK] Testing      SpanningTree");
        for (int r = 0; r < reps; r++) {
            System.out.print(".");
            treeBM.benchmark(molecules, treeStats);
        }
        System.out.println("done");

        System.out.printf("[BENCHMARK] BasicRingTester took on average %.2f ms +/- %.2f\n", bitwiseStats.getMean(), bitwiseStats.getStandardDeviation());
        System.out.printf("[BENCHMARK]      SpanningTree took on average %.2f ms +/- %.2f\n", treeStats.getMean(), treeStats.getStandardDeviation());
        System.out.println("[BENCHMARK]");
        System.out.printf("[BENCHMARK] BasicRingTester took a minimum of %.0f ms and a maximum of %.0f ms\n", bitwiseStats.getMin(), bitwiseStats.getMax());
        System.out.printf("[BENCHMARK]      SpanningTree took a minimum of %.0f ms and a maximum of %.0f ms\n", treeStats.getMin(), treeStats.getMax());
        System.out.println("[BENCHMARK]");

        if (bitwiseStats.getMean() < treeStats.getMean()) {
            double perc = treeStats.getMean() / bitwiseStats.getMean();
            System.out.printf("[BENCHMARK] BasicRingTester was %.2f%% faster\n", perc * 100);
        } else if (bitwiseStats.getMean() > treeStats.getMean()) {
            double perc = bitwiseStats.getMean() / treeStats.getMean();
            System.out.printf("[BENCHMARK] SpanningTree was %.2f%% faster\n", perc * 100);
        } else {
            System.out.println("[BENCHMARK] Methods were equivalent");
        }


    }

    private static void help() {
        System.out.println("[HELP] example usage: java -Xms1G -Xmx2G -jar cycle-detection.jar -i ~/Downloads/Compound_000000001_000025000.sdf");
        System.out.println("[HELP]");
        System.out.println("[HELP] Options:");
        for (Object obj : options.getOptions()) {
            Option opt = (Option) obj; // :(
            System.out.printf("[HELP] %15s %s\n", opt.getOpt() + "|" + opt.getLongOpt(), opt.getDescription());
        }
        System.exit(0);
    }

    private static List<IAtomContainer> loadExtremeMol() {

        try {
            MDLV3000Reader reader = new MDLV3000Reader(BenchmarkSDF.class.getResourceAsStream("extreme.mol"));
            IAtomContainer molecule = reader.read(new AtomContainer());
            List<IAtomContainer> molecules = new ArrayList<IAtomContainer>();
            molecules.add(molecule);
            return molecules;
        } catch (CDKException e) {
            System.err.println("erm:" + e.getMessage());
        }

        return Collections.emptyList();

    }


    private static List<IAtomContainer> load(String sdf) {

        File file = new File(sdf);

        if (file.isDirectory() || !file.exists())
            throw new IllegalArgumentException("file was a directory or did not exist");

        IteratingSDFReader reader = null;
        List<IAtomContainer> molecules = new ArrayList<IAtomContainer>(20000);
        try {
            reader = new IteratingSDFReader(new FileReader(file), SilentChemObjectBuilder.getInstance(), true);
            while (reader.hasNext()) {
                IAtomContainer molecule = reader.next();
                if (molecule.getAtomCount() < filter
                        && !(molecule instanceof IQueryAtomContainer))
                    molecules.add(molecule);
            }

        } catch (FileNotFoundException e) {
            throw new IllegalStateException("unable to read SDF: " + e.getMessage());
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    // can't do anything
                }
        }

        return molecules;
    }

    static class BooleanRingTesterBenchmark extends RingTestBenchmark {
        BooleanRingTesterBenchmark() {
            super("BasicRingTester");
        }

        @Override
        public int benchmark(List<IAtomContainer> molecules, DescriptiveStatistics statistics) {
            int count = 0;

            long start = System.currentTimeMillis();
            for (IAtomContainer molecule : molecules) {
                RingTester tester = new BasicRingTester(molecule);
                for (int i = 0; i < molecule.getAtomCount(); i++) {
                    if (tester.isInRing(i))
                        count++;
                }
            }
            long end = System.currentTimeMillis();
            statistics.addValue(end - start);

            return count;

        }
    }

    static class SimulatedBooleanRingTesterBenchmark extends RingTestBenchmark {
        SimulatedBooleanRingTesterBenchmark() {
            super("BasicRingTester (simulated)");
        }

        @Override
        public int benchmark(List<IAtomContainer> molecules, DescriptiveStatistics statistics) {
            int count = 0;

            long start = System.currentTimeMillis();
            for (IAtomContainer molecule : molecules) {
                RingTester tester = new SimulatedBooleanRingTester(molecule);
                for (int i = 0; i < molecule.getAtomCount(); i++) {
                    if (tester.isInRing(i))
                        count++;
                }
            }
            long end = System.currentTimeMillis();
            statistics.addValue(end - start);

            return count;

        }
    }

    static class SpanningTreeBenchMark extends RingTestBenchmark {

        SpanningTreeBenchMark() {
            super("SpanningTree");
        }

        @Override
        public void prepare(List<IAtomContainer> molecules) {
            for (IAtomContainer molecule : molecules) {
                for (IAtom atom : molecule.atoms())
                    atom.setFlags(new boolean[CDKConstants.MAX_FLAG_INDEX]);
                for (IBond bond : molecule.bonds())
                    bond.setFlags(new boolean[CDKConstants.MAX_FLAG_INDEX]);
            }
        }

        @Override
        public int benchmark(List<IAtomContainer> molecules, DescriptiveStatistics statistics) {
            int count = 0;

            long start = System.currentTimeMillis();
            for (IAtomContainer molecule : molecules) {
                SpanningTree tree = new SpanningTree(molecule);
                IAtomContainer cyclic = tree.getCyclicFragmentsContainer();
                for (IAtom atom : molecule.atoms()) {
                    if (cyclic.contains(atom))
                        count++;
                }
            }
            long end = System.currentTimeMillis();
            statistics.addValue(end - start);

            return count;
        }
    }

    abstract static class RingTestBenchmark {

        private String name;

        public RingTestBenchmark(String name) {
            this.name = name;
        }

        public void prepare(List<IAtomContainer> molecules) {

        }

        public abstract int benchmark(List<IAtomContainer> molecules, DescriptiveStatistics statistics);

    }

}
