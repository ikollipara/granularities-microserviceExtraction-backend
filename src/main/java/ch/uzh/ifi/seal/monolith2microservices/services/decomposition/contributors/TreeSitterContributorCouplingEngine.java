package ch.uzh.ifi.seal.monolith2microservices.services.decomposition.contributors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.seal.monolith2microservices.main.Configs;
import ch.uzh.ifi.seal.monolith2microservices.models.TreeSitterContents;
import ch.uzh.ifi.seal.monolith2microservices.models.couplings.ContributorCoupling;
import ch.uzh.ifi.seal.monolith2microservices.models.evaluation.TreeSitterGranularity;
import ch.uzh.ifi.seal.monolith2microservices.models.git.ChangeEvent;
import ch.uzh.ifi.seal.monolith2microservices.models.git.GitRepository;
import ch.uzh.ifi.seal.monolith2microservices.utils.TreeSitter;

@Service
public class TreeSitterContributorCouplingEngine {

    @Autowired
    Configs config;

    private static Logger logger = LoggerFactory.getLogger(TreeSitterContributorCouplingEngine.class);

    public List<ContributorCoupling> computerCouplings(GitRepository grepo, Repository repo,
            List<ChangeEvent> changeHistory,
            TreeSitterGranularity granularity) throws IOException, GitAPIException {
        List<ContributorCoupling> couplings = new ArrayList<>();

        var authorMap = getContentAuthorMap(grepo, repo, changeHistory, granularity);
        var contents = authorMap.keySet().stream().collect(Collectors.toList());

        logger.info("Content Size: " + contents.size());

        for (int i = 0; i < contents.size(); i++) {
            var first = contents.get(i);
            var firstAuthors = authorMap.get(first);

            for (var second : contents.subList(i + 1, contents.size())) {
                var secondAuthors = authorMap.get(second);

                var similarity = computeAuthorSimilarity(firstAuthors, secondAuthors);

                var coupling = new ContributorCoupling(first.getQualifiedName(), second.getQualifiedName(), similarity);
                coupling.setFirstFileAuthors(firstAuthors);
                coupling.setSecondFileAuthors(secondAuthors);
                couplings.add(coupling);
            }

        }
        logger.info("Contributor Couplings: " + couplings.toString());
        return couplings;
    }

    private Map<TreeSitterContents, List<String>> getContentAuthorMap(
            GitRepository grepo,
            Repository repo, List<ChangeEvent> changeEvents, TreeSitterGranularity granularity)
            throws GitAPIException, IOException {
        Map<TreeSitterContents, List<String>> results = new HashMap<>();
        var git = new Git(repo);
        var pathPrefix = config.localRepositoryDirectory + "/" + grepo.getName() + "_" + grepo.getId();
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
                                var record = results.getOrDefault(c, new ArrayList<>());
                                record.add(personIdent);
                                results.put(c, record);
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

    private int computeAuthorSimilarity(
            List<String> firstAuthors, List<String> secondAuthors) {
        var fst = new HashSet<>(firstAuthors);
        var snd = new HashSet<>(secondAuthors);

        fst.retainAll(snd);
        return fst.size();
    }
}
