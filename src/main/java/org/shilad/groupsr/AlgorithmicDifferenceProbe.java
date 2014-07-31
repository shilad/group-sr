package org.shilad.groupsr;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

import org.wikibrain.sr.evaluation.KnownSimGuess;
import org.wikibrain.sr.evaluation.SimilarityEvaluationLog;
import org.wikibrain.sr.utils.KnownSim;
import org.wikibrain.utils.WpCollectionUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class AlgorithmicDifferenceProbe {
    static final int NUM_RESULTS = 50;

    public void compare(List<File> dirs) throws IOException, ParseException {
        Map<String, List<KnownSimGuess>> guesses = getGuesses(dirs);
        pruneGuesses(guesses);
        showCorrelations(guesses);
        Map<String, Map<String, KnownSimGuess>> guessesByPair = groupGuessesByPair(guesses);
        findDifferences(guessesByPair);
        analyzePairs(guessesByPair);
    }

    private void pruneGuesses(Map<String, List<KnownSimGuess>> guesses) {
        TObjectIntMap counts = new TObjectIntHashMap();
        for (List<KnownSimGuess> l : guesses.values()) {
            for (KnownSimGuess g : l) {
                if (g.hasGuess()) {
                    counts.adjustOrPutValue(g.getUniqueKey(), 1, 1);
                }
            }
        }
        System.out.println("Found " + counts.size() + " concept pairs that appear in at least one algorithm");
        int n = -1;
        for (List<KnownSimGuess> l : guesses.values()) {
            Iterator<KnownSimGuess> iter = l.iterator();
            while (iter.hasNext()) {
                KnownSimGuess g = iter.next();
                if (counts.get(g.getUniqueKey()) != guesses.size()) {
                    iter.remove();
                }
            }
            SimilarityEvaluationLog.setRanks(l);
            if (n < 0) {
                n = l.size();
            } else if (n != l.size()) {
                throw new IllegalStateException();
            }
        }
        System.out.println("Pruned to " + n + " concept pairs that appear in every algorithm");
    }

    private void showCorrelations(Map<String, List<KnownSimGuess>> guesses) {
        SpearmansCorrelation spearmans = new SpearmansCorrelation();
        for (String alg : guesses.keySet()) {
            TDoubleList actual = new TDoubleArrayList();
            TDoubleList predicted = new TDoubleArrayList();
            for (KnownSimGuess g : guesses.get(alg)) {
                actual.add(g.getActual());
                predicted.add(g.getGuess());
            }
            System.out.println("Spearman's correlation for " + alg + " is " +
                    spearmans.correlation(actual.toArray(), predicted.toArray()));
        }
    }

    private void analyzePairs(Map<String, Map<String, KnownSimGuess>> guessesByPair) {
        Map<String, Double> pairScores = new HashMap<String, Double>();
        for (String pairKey : guessesByPair.keySet()) {
            double minError = Double.MAX_VALUE;
            double maxError = -1.0;

            for (String alg : guessesByPair.get(pairKey).keySet()) {
                KnownSimGuess g = guessesByPair.get(pairKey).get(alg);
                minError = Math.min(minError, Math.abs(g.getRankError()));
                maxError = Math.max(maxError, Math.abs(g.getRankError()));
            }
            pairScores.put(pairKey, maxError - minError);
        }

        List<String> ordered = WpCollectionUtils.sortMapKeys(pairScores, true);
        for (int i = 0; i < Math.min(ordered.size(), NUM_RESULTS); i++) {
            showPair(i, guessesByPair.get(ordered.get(i)));
        }
    }

    private void findDifferences(Map<String, Map<String, KnownSimGuess>> guessesByPair) {
        Set<String> algs = new TreeSet<String>();
        Map<String, Map<String, Double>> errorsByPair = new HashMap<String, Map<String, Double>>();
        for (String pairKey : guessesByPair.keySet()) {
            errorsByPair.put(pairKey, new HashMap<String, Double>());
            for (String alg : guessesByPair.get(pairKey).keySet()) {
                KnownSimGuess g = guessesByPair.get(pairKey).get(alg);
                errorsByPair.get(pairKey).put(alg, Math.abs(g.getRankError()));
                algs.add(alg);
            }
        }

        WilcoxonSignedRankTest wilcoxon = new WilcoxonSignedRankTest();
        for (String alg1 : algs) {
            for (String alg2 : algs) {
                if (alg1 == alg2) {
                    continue;
                }
                System.out.println("\nComparing alg " + alg1 + " vs " + alg2);

                int [] winners = new int[3];
                TDoubleList alg1Errors = new TDoubleArrayList();
                TDoubleList alg2Errors = new TDoubleArrayList();
                for (Map<String, Double> errors : errorsByPair.values()) {
                    double e1 = Math.abs(errors.get(alg1));
                    double e2 = Math.abs(errors.get(alg2));
                    if (e1 > e2 + .0000000001) {
                        winners[1]++;      // alg 2 is better
                    } else if (e2 > e1 + 0.00000001) {
                        winners[0]++;      // alg 1 is better
                    } else {
                        winners[2]++;      // they are the same
                    }
                    alg1Errors.add(e1);
                    alg2Errors.add(e2);
                }

                System.out.println(
                        String.format(
                                "\twinners: alg1 %.2f%%, alg2 %.2f%%, tie %.2f%%",
                                100.0 * winners[0] / errorsByPair.size(),
                                100.0 * winners[1] / errorsByPair.size(),
                                100.0 * winners[2] / errorsByPair.size()
                        ));
                double p = wilcoxon.wilcoxonSignedRankTest(alg1Errors.toArray(), alg2Errors.toArray(), false);
                System.out.println("\tWilcoxon signed rank p value: " + p);
            }
        }
    }

    private void showPair(int i, Map<String, KnownSimGuess> guesses) {
        if (guesses.isEmpty()) return;
        KnownSim ks = guesses.values().iterator().next().getKnown();
        System.out.println(String.format("%d. %s vs. %s:", (i+1), ks.phrase1, ks.phrase2));
        List<String> algNames = new ArrayList<String>(guesses.keySet());
        Collections.sort(algNames);
        for (String alg : algNames) {
            KnownSimGuess g = guesses.get(alg);
            System.out.println(String.format(
                    "\t%s: rank_error=%.2f pred_rank=%.2f actual_rank=%.2f guess=%.3f actual=%.3f",
                    alg,
                    g.getRankError(),
                    g.getActualRank(),
                    g.getPredictedRank(),
                    g.getGuess(), g.getActual()));
        }
    }


    private Map<String, Map<String, KnownSimGuess>> groupGuessesByPair(Map<String, List<KnownSimGuess>> guesses) {
        // Build nested map of concept-pair -> alg -> guess
        Map<String, Map<String, KnownSimGuess>> guessesByPair = new HashMap<String, Map<String, KnownSimGuess>>();
        for (String alg : guesses.keySet()) {
            for (KnownSimGuess g : guesses.get(alg)) {
                if (!g.hasGuess()) {
                    continue;
                }
                if (!guessesByPair.containsKey(g.getUniqueKey())) {
                    guessesByPair.put(g.getUniqueKey(), new HashMap<String, KnownSimGuess>());
                }
                guessesByPair.get(g.getUniqueKey()).put(alg, g);
            }
        }
        return guessesByPair;
    }

    private static final List<String> SUMMARY_FIELDS_TO_SKIP = Arrays.asList("disambigConfig", "metricConfig", "resolvePhrases", "dataset");
    public Map<String, List<KnownSimGuess>> getGuesses(List<File> dirs) throws IOException, ParseException {
        Map<String, List<KnownSimGuess>> guesses = new HashMap<String, List<KnownSimGuess>>();
        for (File dir : dirs) {
            File logFile = new File(dir, "overall.log");
            SimilarityEvaluationLog log = SimilarityEvaluationLog.read(logFile);
            System.out.println("summary for " + dir + ":");
            for (Map.Entry<String, String> entry : log.getSummaryAsMap().entrySet()) {
                if (SUMMARY_FIELDS_TO_SKIP.contains(entry.getKey())) {
                    continue;
                }
                System.out.println("\t" + entry.getKey() + ": " + entry.getValue());
            }
            System.out.println("");
            guesses.put(dir.getName(), log.getGuesses());
        }

        return guesses;
    }

    public static void main(String args[]) throws IOException, ParseException {
        if (args.length < 2) {
            System.err.println("Usage: ProbeAlgorithmicDifferences result_dir1 result_dir2 ...");
            System.exit(1);
        }
        // For now, we don't need an env. This may change, though...
        List<File> dirs = new ArrayList<File>();
        for (String path : args) {
            File d = new File(path);
            if (!d.isDirectory() || !new File(d, "overall.log").isFile()) {
                System.err.println("Directory " + d.getAbsolutePath() + " does not exist or does not contain overall.log");
                System.err.println("Usage: ProbeAlgorithmicDifferences result_dir1 result_dir2 ...");
                System.exit(1);
            }
            dirs.add(d);
        }

        AlgorithmicDifferenceProbe pad = new AlgorithmicDifferenceProbe();
        pad.compare(dirs);
    }
}
