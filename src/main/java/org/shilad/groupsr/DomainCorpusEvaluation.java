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

import java.io.File;
import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class DomainCorpusEvaluation {
    public static String [] FIELDS = {
            "biology",
            "psychology",
            "history"
    };

    public DomainCorpusEvaluation() {}

    public void evaluate(String field, File pathConf) throws ConfigurationException, IOException, DaoException, WikiBrainException {
        Env env = new EnvBuilder().setConfigFile(pathConf).build();
        try {
            Language language = env.getLanguages().getDefaultLanguage();
            File file = new File("dat/specific-" + field + ".txt");
            DatasetDao dao = env.getConfigurator().get(DatasetDao.class);
            Dataset ds = dao.read(language, file);
            File dir = new File("results");
            dir.mkdirs();
            SimilarityEvaluator evaluator = new SimilarityEvaluator(dir);
            evaluator.addCrossfolds(ds, 7);
            evaluator.evaluate(new ConfigMonolingualSRFactory(language, env.getConfigurator(), "ensemble"));
        } finally {
            env.close();
        }
    }

    public static void main(String args[]) throws WikiBrainException, DaoException, ConfigurationException, IOException {
        DomainCorpusEvaluation evaluator = new DomainCorpusEvaluation();
        File dir = new File(args[0]);
        for (String field : FIELDS) {
            File conf = new File(dir, field + ".conf");
            if (!conf.isFile()) {
                System.err.println("conf file " + conf.getAbsolutePath() + " does not exist");
                System.exit(1);
            }
            evaluator.evaluate(field, conf);
        }
    }
}
