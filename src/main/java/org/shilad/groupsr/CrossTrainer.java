package org.shilad.groupsr;

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
import org.wikibrain.sr.evaluation.Split;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class CrossTrainer {

    private final Env env;
    private final Language language;

    public CrossTrainer(Env env) {
        this.env = env;
        this.language = env.getLanguages().getDefaultLanguage();
    }

    public void evaluate() throws ConfigurationException, DaoException, IOException, WikiBrainException {
        File dir = new File("results");
        dir.mkdirs();
        SimilarityEvaluator evaluator = new SimilarityEvaluator(dir);

        DatasetDao dao = env.getConfigurator().get(DatasetDao.class);
        Dataset turker = dao.read(language, new File("dat/specific-mturk.txt"));
        Dataset scholar = dao.read(language, new File("dat/specific-scholar.txt"));
        Dataset scholarIn = dao.read(language, new File("dat/specific-scholar-in.txt"));
        List<Dataset> datasets = Arrays.asList(turker, scholar, scholarIn);

        for (Dataset train : datasets) {
            for (Dataset test : datasets) {
                String name = "train-" + train.getName() + "::test-" + test.getName();
                evaluator.addSplit(new Split(name, name, train, test));
            }
        }

        evaluator.evaluate(new ConfigMonolingualSRFactory(language, env.getConfigurator(), "ensemble"));
    }

    public static void main(String args[]) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        CrossTrainer xt = new CrossTrainer(env);
        xt.evaluate();
        env.close();
    }
}
