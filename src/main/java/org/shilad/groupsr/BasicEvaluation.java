package org.shilad.groupsr;

import org.apache.commons.cli.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.dataset.DatasetDao;
import org.wikibrain.sr.evaluation.ConfigMonolingualSRFactory;
import org.wikibrain.sr.evaluation.SimilarityEvaluator;

import java.io.File;
import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class BasicEvaluation {
    public static final String [] DATASETS = {
        "all-all-scholar.txt",
        "all-biology.txt",
        "all-history.txt",
        "all-mturk.txt",
        "all-psychology.txt",
        "all-scholar.txt",
        "all-scholar-all.txt",
        "all-scholar-in.txt",
        "general-biology.txt",
        "general-history.txt",
        "general-mturk.txt",
        "general-psychology.txt",
        "general-scholar.txt",
        "general-scholar-all.txt",
        "general-scholar-in.txt",
        "specific-biology.txt",
        "specific-history.txt",
        "specific-mturk.txt",
        "specific-psychology.txt",
        "specific-scholar.txt",
        "specific-scholar-all.txt",
        "specific-scholar-in.txt"
    };

    private final Env env;
    private final Language language;

    public BasicEvaluation(Env env) {
        this.env = env;
        this.language = env.getLanguages().getDefaultLanguage();
    }

    public void testAll(String metricName) throws ConfigurationException, DaoException, IOException, WikiBrainException {
        for (String datasetName : DATASETS) {
            testOne(metricName, datasetName);
        }
    }

    public void testOne(String metricName, String datasetName) throws ConfigurationException, DaoException, IOException, WikiBrainException {
        File file = new File("dat/" + datasetName);
        DatasetDao dao = env.getConfigurator().get(DatasetDao.class);
        Dataset ds = dao.read(language, file);
        File dir = new File("results");
        dir.mkdirs();
        SimilarityEvaluator evaluator = new SimilarityEvaluator(dir);
        evaluator.addCrossfolds(ds, 4);
        evaluator.evaluate(new ConfigMonolingualSRFactory(language, env.getConfigurator(), metricName));
    }

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException, WikiBrainException {
        Options options = new Options();
        options.addOption(
            new DefaultOptionBuilder()
                    .hasArg()
                    .withLongOpt("metric")
                    .withDescription("set a local metric")
                    .create("m"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("BasicEvaluation", options);
            System.exit(1);
            return; // to appease the compiler
        }

        String metricName = cmd.getOptionValue("m", "ensemble");

        Env env = new EnvBuilder(cmd).build();
        BasicEvaluation basicEvaluation = new BasicEvaluation(env);
        basicEvaluation.testAll(metricName);
    }
}
