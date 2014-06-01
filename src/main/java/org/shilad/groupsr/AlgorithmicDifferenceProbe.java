package org.shilad.groupsr;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TDoubleDoubleMap;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.apache.commons.math3.stat.regression.SimpleRegression;
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
        Map<String, Map<String, KnownSimGuess>> guessesByPair = groupGuessesByPair(guesses);
        analyzePairs(guessesByPair);
    }

    private void analyzePairs(Map<String, Map<String, KnownSimGuess>> guessesByPair) {

        // accumulate SR scores for each algorithm
        Map<String, TDoubleList> algScores = new HashMap<String, TDoubleList>();
        algScores.put("actual", new TDoubleArrayList());
        for (Map<String, KnownSimGuess> pairGuesses : guessesByPair.values()) {
            boolean firstAlg = true;
            for (String alg : pairGuesses.keySet()) {
                if (!algScores.containsKey(alg)) {
                    algScores.put(alg, new TDoubleArrayList());
                }
                algScores.get(alg).add(pairGuesses.get(alg).getGuess());
                if (firstAlg) {
                    algScores.get("actual").add(pairGuesses.get(alg).getActual());
                    firstAlg = false;
                }
            }
        }


        // create mapping from SR scores to ranks
        NaturalRanking nr = new NaturalRanking(TiesStrategy.MAXIMUM);
        Map<String, TDoubleDoubleMap> scoreToRank = new HashMap<String, TDoubleDoubleMap>();
        for (String alg : algScores.keySet()) {
            double [] ranks = nr.rank(algScores.get(alg).toArray());
            scoreToRank.put(alg, new TDoubleDoubleHashMap());
            for (int i = 0; i < ranks.length; i++) {
                scoreToRank.get(alg).put(algScores.get(alg).get(i), ranks[i]);
            }
        }

        Map<String, Double> pairScores = new HashMap<String, Double>();
        for (String pairKey : guessesByPair.keySet()) {
            double minRank = Double.MAX_VALUE;
            double maxRank = -1.0;

            for (String alg : guessesByPair.get(pairKey).keySet()) {
                KnownSimGuess g = guessesByPair.get(pairKey).get(alg);
                double r = scoreToRank.get(alg).get(g.getGuess());
                minRank = Math.min(minRank, r);
                maxRank = Math.max(maxRank, r);
            }
            pairScores.put(pairKey, maxRank - minRank);
        }

        List<String> ordered = WpCollectionUtils.sortMapKeys(pairScores, true);
        for (int i = 0; i < Math.min(ordered.size(), NUM_RESULTS); i++) {
            showPair(i, guessesByPair.get(ordered.get(i)), scoreToRank);
        }
    }

    private void showPair(int i, Map<String, KnownSimGuess> guesses, Map<String, TDoubleDoubleMap> scoreToRank) {
        if (guesses.isEmpty()) return;
        KnownSim ks = guesses.values().iterator().next().getKnown();
        System.out.println(String.format("%d. %s vs. %s:", (i+1), ks.phrase1, ks.phrase2));
        List<String> algNames = new ArrayList<String>(guesses.keySet());
        Collections.sort(algNames);
        for (String alg : algNames) {
            KnownSimGuess g = guesses.get(alg);
            System.out.println(String.format(
                    "\t%s: pred_rank=%.2f actual_rank=%.2f guess=%.3f actual=%.3f",
                    alg,
                    scoreToRank.get(alg).get(g.getGuess()),
                    scoreToRank.get("actual").get(g.getActual()),
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

        System.out.println("Found " + guessesByPair.size() + " concept-pairs that appear in at least one directory");

        // Remove things that don't appear in all directories
        int removed = 0;
        Iterator<Map<String, KnownSimGuess>> valueIter = guessesByPair.values().iterator();
        while (valueIter.hasNext()) {
            if (valueIter.next().size() != guesses.size()) {
                valueIter.remove();
                removed++;
            }
        }

        System.out.println("Removed " + removed + " concept pairs that don't appear in every directory. Retained " + guessesByPair.size() + ".");
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
            guesses.put(dir.getName(), fitGuesses(log.getGuesses()));
        }

        return guesses;
    }

    private List<KnownSimGuess> fitGuesses(List<KnownSimGuess> guesses) {
        SimpleRegression reg = new SimpleRegression();
        int n = 0;
        for (KnownSimGuess g : guesses) {
            if (g.hasGuess()) {
                reg.addData(g.getGuess(), g.getActual());
                n++;
            }
        }
        if (n < 5) {
            return guesses;
        }
        List<KnownSimGuess> fitted = new ArrayList<KnownSimGuess>();
        for (KnownSimGuess g : guesses) {
            if (g.hasGuess()) {
                fitted.add(new KnownSimGuess(
                        g.getKnown(),
                        reg.getIntercept() + reg.getSlope() * g.getGuess()
                ));
            } else {
                fitted.add(g);
            }
        }
        return fitted;
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
