package ch.uzh.ifi.seal.monolith2microservices.services.decomposition.logicalcoupling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import ch.uzh.ifi.seal.monolith2microservices.main.Configs;
import ch.uzh.ifi.seal.monolith2microservices.models.TreeSitterContents;
import ch.uzh.ifi.seal.monolith2microservices.models.couplings.LogicalCoupling;
import ch.uzh.ifi.seal.monolith2microservices.models.evaluation.TreeSitterGranularity;
import ch.uzh.ifi.seal.monolith2microservices.models.git.ChangeEvent;
import ch.uzh.ifi.seal.monolith2microservices.models.git.GitRepository;
import ch.uzh.ifi.seal.monolith2microservices.utils.Hash;
import ch.uzh.ifi.seal.monolith2microservices.utils.TreeSitter;

@Service
public class TreeSitterLogicalCouplingEngine {

    @Autowired
    Configs config;

    private static Logger logger = LoggerFactory.getLogger(TreeSitterLogicalCouplingEngine.class);
    private int changeEventCounter;
    private Map<String, LogicalCoupling> resultMap;

    public List<LogicalCoupling> computeCouplings(
            GitRepository repo, List<ChangeEvent> changeHistory, int intervalInSeconds,
            TreeSitterGranularity granularity)
            throws IOException {
        resultMap = new HashMap<>();
        var tStart = extractEarliestTimestamp(changeHistory);
        var tEnd = extractLatestTimestamp(changeHistory);
        Collections.reverse(changeHistory);

        for (int tCurrent = tStart; tCurrent < tEnd; tCurrent += intervalInSeconds) {
            var changeEvents = extractChangeEvents(changeHistory, tCurrent, tCurrent + intervalInSeconds);

            if (changeEvents.isEmpty())
                continue;

            ImmutableSet<TreeSitterContents> set = ImmutableSet
                    .copyOf(getChangedContents(repo, changeEvents, granularity));
            if (set.size() >= 2) {
                for (Set<TreeSitterContents> el : Sets.combinations(set, 2)) {
                    var coupling = generateLogicalCoupling(el, tCurrent, tCurrent + intervalInSeconds);
                    resultMap.put(coupling.getHash(), coupling);
                }
            }
        }

        return resultMap.values().stream().collect(Collectors.toList());
    }

    private int extractEarliestTimestamp(List<ChangeEvent> changeHistory) {
        return changeHistory.get(changeHistory.size() - 1).getTimestampInSeconds();
    }

    private int extractLatestTimestamp(List<ChangeEvent> changeHistory) {
        return changeHistory.get(0).getTimestampInSeconds();
    }

    private List<ChangeEvent> extractChangeEvents(List<ChangeEvent> changeHistory, int tStart, int tEnd) {
        List<ChangeEvent> results = new ArrayList<>();
        var time = tStart;

        while (changeEventCounter < changeHistory.size() && time < tEnd) {
            var currentEvent = changeHistory.get(changeEventCounter);

            if ((tStart <= currentEvent.getTimestampInSeconds()) && (currentEvent.getTimestampInSeconds() < tEnd)) {
                results.add(currentEvent);
                changeEventCounter++;
            } else {
                time = currentEvent.getTimestampInSeconds();
            }
        }

        return results;
    }

    private List<TreeSitterContents> getChangedContents(GitRepository repo, List<ChangeEvent> changeHistory,
            TreeSitterGranularity granularity) throws IOException {
        List<TreeSitterContents> results = new ArrayList<>();

        var pathPrefix = config.localRepositoryDirectory + "/" + repo.getName() + "_" + repo.getId();

        for (var event : changeHistory) {
            for (var fileName : event.getChangedFileNames()) {
                var parser = TreeSitter.getLangFromFilename(fileName).map(TreeSitter::getParser);
                var contents = Files.readString(Paths.get(pathPrefix + "/" + fileName));
                parser.ifPresent(p -> {
                    var tree = p.parseString(null, contents);
                    try {
                        results.addAll(TreeSitter.getMatches(tree, p, granularity, contents, fileName)
                                .collect(Collectors.toList()));
                    } catch (IOException e) {
                        logger.error(e.toString());
                    }
                });
            }
        }

        // for (var event : changeHistory) {
        // var commit = event.getCommitObject();
        // for (var diff : event.getChangedfiles()) {
        // List<TreeSitterContents> result;
        // if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
        // result = handleAdd(repo, commit, diff, granularity);
        // results.addAll(result);
        // } else if (diff.getChangeType() == DiffEntry.ChangeType.DELETE) {
        // var delresult = handleDelete(repo, commit, diff, granularity);
        // results.removeAll(delresult);
        // } else if (diff.getChangeType() == DiffEntry.ChangeType.MODIFY) {
        // result = handleModify(repo, commit, diff, granularity);
        // results.addAll(result);
        // }
        // }
        // }

        return results;
    }

    private List<TreeSitterContents> handleModify(Repository repo, RevCommit commit, DiffEntry diff,
            TreeSitterGranularity granularity) throws IOException {
        return getFileFromCommit(repo, commit, diff.getNewPath(), granularity);
    }

    private List<TreeSitterContents> handleAdd(Repository repo, RevCommit commit, DiffEntry diff,
            TreeSitterGranularity granularity) throws IOException {
        return getFileFromCommit(repo, commit, diff.getNewPath(), granularity);
    }

    private List<TreeSitterContents> handleDelete(Repository repo, RevCommit commit, DiffEntry diff,
            TreeSitterGranularity granularity) throws IOException {
        return getFileFromCommit(repo, commit, diff.getOldPath(), granularity);
    }

    private List<TreeSitterContents> getFileFromCommit(Repository repo, RevCommit commit, String path,
            TreeSitterGranularity granularity) throws IOException {
        List<TreeSitterContents> results = new ArrayList<>();
        try (var treeWalk = TreeWalk.forPath(repo, path, commit.getTree())) {
            if (treeWalk == null)
                return results;

            var objectId = treeWalk.getObjectId(0);

            try (var reader = repo.newObjectReader()) {
                var rawContents = new String(reader.open(objectId).getBytes(), StandardCharsets.UTF_8);
                var parser = TreeSitter.getLangFromFilename(path).map(TreeSitter::getParser);
                parser.ifPresent(p -> {
                    try {
                        var tree = p.parseString(null, rawContents);
                        results.addAll(TreeSitter.getMatches(tree, p, granularity, rawContents, path)
                                .collect(Collectors.toList()));
                    } catch (IOException e) {
                        logger.error(e.toString());
                    }
                });
            }
        }
        return results;
    }

    private Optional<RevCommit> getFirstParent(Repository repo, RevCommit commit) throws IOException {
        if (commit.getParentCount() == 0)
            return Optional.empty();

        try (var revWalk = new RevWalk(repo)) {
            return Optional.of(revWalk.parseCommit(commit.getParent(0).getId()));
        }
    }

    private LogicalCoupling generateLogicalCoupling(Set<TreeSitterContents> contents, int tStart, int tEnd) {
        TreeSitterContents[] aitems = new TreeSitterContents[contents.size()];
        aitems = contents.toArray(aitems);
        var items = Arrays.asList(aitems);

        Collections.sort(items);
        var key = String.join("\\?", items.stream().map(i -> i.getQualifiedName()).collect(Collectors.toList()));
        var hash = new Hash(key).get();

        return Optional.ofNullable(resultMap.get(hash))
                .map(e -> {
                    e.incrementScore();
                    return e;
                })
                .orElseGet(() -> {
                    var newCoupling = new LogicalCoupling(items.get(0).getQualifiedName(),
                            items.get(1).getQualifiedName(), 1);
                    newCoupling.setHash(hash);
                    newCoupling.setStartTimestamp(tStart);
                    newCoupling.setEndTimestamp(tEnd);

                    return newCoupling;
                });

    }
}
