package ch.uzh.ifi.seal.monolith2microservices.services.evaluation;

import ch.uzh.ifi.seal.monolith2microservices.main.Configs;
import ch.uzh.ifi.seal.monolith2microservices.models.evaluation.TreeSitterGranularity;
import ch.uzh.ifi.seal.monolith2microservices.models.git.GitRepository;
import ch.uzh.ifi.seal.monolith2microservices.models.graph.Component;
import ch.uzh.ifi.seal.monolith2microservices.services.decomposition.semanticcoupling.tfidf.TfIdfWrapper;
import ch.uzh.ifi.seal.monolith2microservices.utils.ClassContentFilter;
import ch.uzh.ifi.seal.monolith2microservices.utils.FilterInterface;
import ch.uzh.ifi.seal.monolith2microservices.utils.TreeSitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Genc on 15.01.2017.
 */
@Service
public class MicroserviceSimilarityService {

    private static Logger logger = LoggerFactory.getLogger(MicroserviceSimilarityService.class);

    @Autowired
    Configs configs;

    FilterInterface filterInterface = new ClassContentFilter();

    public double computeServiceSimilarity(GitRepository repo, Component firstMicroservice,
            Component secondMicroservice, TreeSitterGranularity granularity, boolean useTreeSitter) throws IOException {
        List<String> firstServiceContent = computeTokenizedServiceContent(repo, firstMicroservice, granularity,
                useTreeSitter);
        List<String> secondServiceContent = computeTokenizedServiceContent(repo, secondMicroservice, granularity,
                useTreeSitter);
        logger.info("First Service Content Line 1: " + firstServiceContent.get(0));
        logger.info("Second Service Content Line 1: " + secondServiceContent.get(0));
        return TfIdfWrapper.computeSimilarity(firstServiceContent, secondServiceContent);
    }

    private List<String> computeTokenizedServiceContent(GitRepository repo, Component microservice,
            TreeSitterGranularity granularity, boolean useTreeSitter) throws IOException {
        if (!useTreeSitter) {
            List<String> filePaths = microservice.getFilePaths();
            String pathPrefix = configs.localRepositoryDirectory + "/" + repo.getName() + "_" + repo.getId();

            List<String> content = new ArrayList<>();

            for (String filePath : filePaths) {
                String rawContent = getRawFileContent(Paths.get(pathPrefix + "/" + filePath));
                content.addAll(filterInterface.filterFileContent(rawContent));
            }
            return content;

        } else {
            List<String> filePaths = microservice.getFilePaths();
            String pathPrefix = configs.localRepositoryDirectory + "/" + repo.getName() + "_" + repo.getId();

            List<String> content = new ArrayList<>();

            for (var filepath : filePaths) {
                var parts = filepath.split("::");
                var filename = parts[0];
                var contents = Files.readString(Paths.get(pathPrefix + "/" + filename));
                var parser = TreeSitter.getLangFromFilename(filename).map(TreeSitter::getParser);
                parser.ifPresent(p -> {
                    var tree = p.parseString(null, contents);
                    try {
                        var matches = TreeSitter.getMatches(tree, p, granularity, contents, filename);
                        String[] lines = matches.filter(c -> c.getQualifiedName().contains(filepath)).findFirst()
                                .map(c -> c.getRawContents().split("\n")).orElse(new String[0]);
                        content.addAll(Arrays.asList(lines));
                    } catch (IOException e) {
                        logger.error(e.toString());
                    }
                });
            }
            return content;
        }
    }

    private String getRawFileContent(Path path) throws IOException {
        BufferedReader reader = Files.newBufferedReader(path);
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

}
