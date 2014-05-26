package org.shilad.groupsr;

import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.CategoryGraph;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.sr.category.CategoryBfs;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class Importer {
    private static final Logger LOG = Logger.getLogger(Importer.class.getName());

    private static final String[] CATEGORIES = new String[] { "History", "Psychology", "Biology" };

    private final Env fullEnv;
    private final Language language;
    private final File conf;

    public Importer(File conf, Env env) {
        this.conf = conf;
        this.fullEnv = env;
        this.language = env.getLanguages().getDefaultLanguage();
    }

    public void prepareSubsets() throws ConfigurationException, DaoException, IOException {
        String template = WpIOUtils.resourceToString("/src/main/resources/template.conf");
        Path baseDir = Paths.get(fullEnv.getConfiguration().get().getString("baseDir"));

        String script = "";
        for (String category : CATEGORIES) {
            Set<Integer> childIds = getPagesInCategory(category);
            Path newDir = baseDir.resolve(category.toLowerCase());
            FileUtils.deleteQuietly(newDir.toFile());
            newDir.toFile().mkdirs();
            FileUtils.copyDirectory(
                    baseDir.resolve("download").toFile(),
                    newDir.resolve("download").toFile());

            BufferedWriter writer = WpIOUtils.openWriter(new File(newDir.toFile(), "validIds.txt"));
            for (int id : childIds) {
                writer.write(id + "\n");
            }
            writer.close();

            String confStr = "";
            if (conf != null) {
                for (String line : FileUtils.readLines(conf)) {
                    if (line.contains("jdbc:postgresql")) {
                        int lastQuote = line.lastIndexOf("\"");
                        if (lastQuote >= 0) {
                            line = line.substring(0, lastQuote) + "_" + category.toLowerCase() + line.substring(lastQuote);
                        } else {
                            line += "_" + category.toLowerCase();
                        }
                    }
                    if (!line.trim().startsWith("baseDir ")) {
                        confStr += line + "\n";
                    }
                }
            }
            confStr = "baseDir : " + newDir.toFile().getAbsolutePath() + "\n" + confStr;
            confStr += "\n" + template + "\n";
            File newConf = new File(category.toLowerCase() + ".conf");
            FileUtils.write(newConf, confStr);

            script += "./wb-java.sh org.wikibrain.dao.load.PipelineLoader -c " + newConf.getAbsolutePath() + " -l " + language.getLangCode() + " &&\n";
        }
        script += "true\n";

        FileUtils.write(new File("import.sh"), script);
    }

    public Set<Integer> getPagesInCategory(String category) throws ConfigurationException, DaoException {
        LocalCategoryMemberDao catDao = fullEnv.getConfigurator().get(LocalCategoryMemberDao.class);
        LocalPageDao pageDao = fullEnv.getConfigurator().get(LocalPageDao.class);

        int catId = pageDao.getIdByTitle("Category:" + category, language, NameSpace.CATEGORY);
        if (catId < 0) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        CategoryGraph graph = catDao.getGraph(language);
        CategoryBfs bfs = new CategoryBfs(graph, catId, NameSpace.CATEGORY, language, 100000, null, catDao, -1);
        Set<Integer> pageIds = new HashSet<Integer>();
        int steps = 0;
        while (true) {
            steps++;
            CategoryBfs.BfsVisited visited = bfs.step();
            if (visited.cats.isEmpty()) {
                break;
            }
            double distance = Double.POSITIVE_INFINITY;
            for (int id : visited.cats.keys()) {
                int childId = graph.catIds[id];
//                System.out.println("\tvisited " + pageDao.getById(language, childId) + " distance " + bfs.getCategoryDistance(childId));
                distance = Math.min(distance, bfs.getCategoryDistance(childId));
            }
            for (int pageId : visited.pages.keys()) {
//                System.out.println("\tvisited " + pageDao.getById(language, pageId) + " distance " + bfs.getCategoryDistance(pageId));
                pageIds.add(pageId);
            }
            if (distance >= 0.30) {
                break;
            }
        }
        LOG.info("found " + pageIds.size() + " pages for " + category + " associated with " + steps + " categories");
        return pageIds;
    }

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        File conf = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-c")) {
                conf = new File(args[i+1]);
            }
        }
        Env env = EnvBuilder.envFromArgs(args);
        Importer importer = new Importer(conf, env);
        importer.prepareSubsets();
    }
}
