package ch.uzh.ifi.seal.monolith2microservices.services.evaluation;

import ch.uzh.ifi.seal.monolith2microservices.main.Configs;
import ch.uzh.ifi.seal.monolith2microservices.models.TreeSitterContents;
import ch.uzh.ifi.seal.monolith2microservices.models.evaluation.MicroserviceMetrics;
import ch.uzh.ifi.seal.monolith2microservices.models.evaluation.TreeSitterGranularity;
import ch.uzh.ifi.seal.monolith2microservices.models.git.ChangeEvent;
import ch.uzh.ifi.seal.monolith2microservices.models.git.GitRepository;
import ch.uzh.ifi.seal.monolith2microservices.models.graph.ClassNode;
import ch.uzh.ifi.seal.monolith2microservices.models.graph.Component;
import ch.uzh.ifi.seal.monolith2microservices.services.git.AuthorService;
import ch.uzh.ifi.seal.monolith2microservices.utils.TreeSitter;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by gmazlami on 1/15/17.
 */
@Service
public class MicroserviceEvaluationService {

    private Logger logger = LoggerFactory.getLogger(MicroserviceEvaluationService.class);

    @Autowired
    private AuthorService authorService;

    @Autowired
    private Configs config;

    public MicroserviceMetrics from(Component microservice, GitRepository repo, List<ChangeEvent> history,
            TreeSitterGranularity granularity, Boolean isUsingTreeSitter, Repository repository)
            throws IOException, GitAPIException {
        if (!isUsingTreeSitter) {
            MicroserviceMetrics metrics = new MicroserviceMetrics(microservice);

            Map<String, Set<String>> fileAuthorMap = generateAuthorMap(history);

            metrics.setContributors(computeAuthorSet(microservice, fileAuthorMap));
            metrics.setSizeLoc(computeSizeInLoc(microservice, repo, granularity, false));
            return metrics;
        } else {
            var metrics = new MicroserviceMetrics(microservice);
            Map<String, Set<String>> contentAuthorMap = granularity.equals(TreeSitterGranularity.MODULE)
                    ? generateAuthorMap(history)
                    : generateContentAuthorMap(repo, repository, history, granularity);
            metrics.setContributors(computeAuthorSet(microservice, contentAuthorMap));
            metrics.setSizeLoc(computeSizeInLoc(microservice, repo, granularity, true));
            return metrics;
        }
    }

    private Map<String, Set<String>> generateAuthorMap(List<ChangeEvent> history) {
        Map<String, Set<String>> fileAuthorMap = new HashMap<>();

        for (ChangeEvent event : history) {
            for (String fileName : event.getChangedFileNames()) {
                if (fileAuthorMap.get(fileName) == null) {
                    Set<String> authorSet = new HashSet<>();
                    authorSet.add(event.getAuthorEmailAddress());
                    fileAuthorMap.put(fileName, authorSet);
                } else {
                    Set<String> authorSet = fileAuthorMap.get(fileName);
                    authorSet.add(event.getAuthorEmailAddress());
                    fileAuthorMap.put(fileName, authorSet);
                }
            }
        }

        return fileAuthorMap;
    }

    private Map<String, Set<String>> generateContentAuthorMap(
            GitRepository grepo,
            Repository repo, List<ChangeEvent> changeEvents, TreeSitterGranularity granularity)
            throws GitAPIException, IOException {
        Map<String, Set<String>> results = new HashMap<>();
        var pathPrefix = config.localRepositoryDirectory + "/" + grepo.getName() + "_" + grepo.getId();
        var git = new Git(repo);
        for (var event : changeEvents) {
            for (var filename : event.getChangedFileNames()) {
                var blame = git.blame().setFilePath(filename).call();
                var parser = TreeSitter.getLangFromFilename(filename).map(TreeSitter::getParser);
                var contents = Files.readString(Paths.get(pathPrefix + "/" + filename));
                parser.ifPresent(p -> {
                    var tree = p.parseString(null, contents);
                    try {
                        TreeSitter.getMatches(tree, p, granularity, contents, filename).forEach(c -> {
                            var start = c.getNode().getStartPoint().getRow();
                            var end = c.getNode().getEndPoint().getRow();

                            for (int i = start; i < end; i++) {
                                var personIdent = blame.getSourceCommitter(i).getEmailAddress();
                                var record = results.getOrDefault(c.getQualifiedName(), new HashSet<>());
                                record.add(personIdent);
                                results.put(c.getQualifiedName(), record);
                            }
                        });
                        ;
                    } catch (IOException e) {
                        logger.error(e.toString());
                    }
                });
            }
        }
        git.close();
        return results;
    }

    private List<TreeSitterContents> extractContentsFromCommitFile(Repository repo, RevCommit commit, String filename,
            TreeSitterGranularity granularity) throws IOException {
        List<TreeSitterContents> results = new ArrayList<>();
        try (var treeWalk = TreeWalk.forPath(repo, filename, commit.getTree())) {
            if (treeWalk == null)
                return results;

            var objectId = treeWalk.getObjectId(0);
            try (var reader = repo.newObjectReader()) {
                var rawContents = new String(reader.open(objectId).getBytes(), StandardCharsets.UTF_8);
                var parser = TreeSitter.getLangFromFilename(filename).map(TreeSitter::getParser);
                parser.ifPresent(p -> {
                    try {
                        var tree = p.parseString(null, rawContents);
                        results.addAll(TreeSitter.getMatches(tree, p, granularity, rawContents, filename)
                                .collect(Collectors.toList()));
                    } catch (IOException e) {
                        logger.error(e.toString());
                    }
                });
            }
        }
        return results;
    }

    private Set<String> computeAuthorSet(Component microservice, Map<String, Set<String>> authorMap) {
        Set<String> authorSet = new HashSet<>();

        for (ClassNode node : microservice.getNodes()) {
            Set<String> authors = authorMap.get(node.getId());
            if (authors != null) {
                authorSet.addAll(authors);
            }
        }

        return authorSet;
    }

    private int computeSizeInLoc(Component microservice, GitRepository repo, TreeSitterGranularity granularity,
            Boolean useTreeSitter) throws IOException {

        if (!useTreeSitter) {
            List<String> filePaths = new ArrayList<>();
            microservice.getNodes().forEach(node -> filePaths.add(node.getId()));
            String pathPrefix = config.localRepositoryDirectory + "/" + repo.getName() + "_" + repo.getId();

            int lineCounter = 0;

            for (String filePath : filePaths) {
                BufferedReader reader = Files.newBufferedReader(Paths.get(pathPrefix + "/" + filePath));
                while (reader.readLine() != null) {
                    lineCounter++;
                }
            }

            return lineCounter;

        } else {
            var pathPrefix = config.localRepositoryDirectory + "/" + repo.getName() + "_" + repo.getId();
            int lineCounter = 0;
            for (var node : microservice.getNodes()) {
                var parts = node.getId().split("::");
                var filename = parts[0];
                var contents = Files.readString(Paths.get(pathPrefix + "/" + filename));
                var parser = TreeSitter.getLangFromFilename(filename).map(TreeSitter::getParser);
                lineCounter += parser.map(p -> {
                    var tree = p.parseString(null, contents);
                    try {
                        var matches = TreeSitter.getMatches(tree, p, granularity,
                                Paths.get(pathPrefix + "/" + filename));
                        return matches.filter(c -> c.getQualifiedName().contains(node.getId()))
                                .findFirst()
                                .map(c -> c.getRawContents().split("\n").length).orElse(0);
                    } catch (IOException e) {
                        logger.error(e.toString());
                        return 0;
                    }
                }).orElse(0);
            }
            return lineCounter;
        }
    }

}
