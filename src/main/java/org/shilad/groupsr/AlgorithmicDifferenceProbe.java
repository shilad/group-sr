package org.shilad.groupsr;

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
        Map<String, Double> pairScores = new HashMap<String, Double>();
        for (String pairKey : guessesByPair.keySet()) {
            double minError = Double.MAX_VALUE;
            double maxError = -1.0;

            for (KnownSimGuess g : guessesByPair.get(pairKey).values()) {
                double e = Math.abs(g.getError());
                minError = Math.min(minError, e);
                maxError = Math.max(maxError, e);
            }
            pairScores.put(pairKey, maxError - minError);
        }

        List<String> ordered = WpCollectionUtils.sortMapKeys(pairScores, true);
        for (int i = 0; i < Math.min(ordered.size(), NUM_RESULTS); i++) {
            showPair(i, guessesByPair.get(ordered.get(i)));
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
                    "\t%s: err=%.3f, pred=%.3f, actual=%.3f",
                    alg, g.getError(), g.getGuess(), g.getActual()));
        }
    }


    private Map<String, Map<String, KnownSimGuess>> groupGuessesByPair(Map<String, List<KnownSimGuess>> guesses) {
        // Build nested map of concept-pair -> alg -> guess
        Map<String, Map<String, KnownSimGuess>> guessesByPair = new HashMap<String, Map<String, KnownSimGuess>>();
        for (String alg : guesses.keySet()) {
            for (KnownSimGuess g : guesses.get(alg)) {
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

    public Map<String, List<KnownSimGuess>> getGuesses(List<File> dirs) throws IOException, ParseException {
        Map<String, List<KnownSimGuess>> guesses = new HashMap<String, List<KnownSimGuess>>();
        for (File dir : dirs) {
            File logFile = new File(dir, "overall.log");
            SimilarityEvaluationLog log = SimilarityEvaluationLog.read(logFile);
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
