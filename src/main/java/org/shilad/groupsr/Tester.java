package org.shilad.groupsr;

import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
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
public class Tester {
    public static final String [] DATASETS = {
        "all-all-scholar.txt",
        "all-biology.txt",
        "all-history.txt",
        "all-mturk.txt",
        "all-psychology.txt",
        "all-scholar.txt",
        "all-scholar-all.txt",
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

    public Tester(Env env) {
        this.env = env;
        this.language = env.getLanguages().getDefaultLanguage();
    }

    public void testAll() throws ConfigurationException, DaoException, IOException, WikiBrainException {
        for (String name : DATASETS) {
            testOne(name);
        }
    }

    public void testOne(String name) throws ConfigurationException, DaoException, IOException, WikiBrainException {
        File file = new File("dat/" + name);
        DatasetDao dao = env.getConfigurator().get(DatasetDao.class);
        Dataset ds = dao.read(language, file);
        File dir = new File("results");
        dir.mkdirs();
        SimilarityEvaluator evaluator = new SimilarityEvaluator(dir);
        evaluator.addCrossfolds(ds, 7);
        evaluator.evaluate(new ConfigMonolingualSRFactory(language, env.getConfigurator(), "ensemble"));
    }

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException, WikiBrainException {
        Env env = EnvBuilder.envFromArgs(args);
        Tester tester = new Tester(env);
        tester.testAll();
    }
}