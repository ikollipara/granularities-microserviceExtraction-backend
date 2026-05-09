package ch.uzh.ifi.seal.monolith2microservices.services.decomposition.semanticcoupling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.seal.monolith2microservices.main.Configs;
import ch.uzh.ifi.seal.monolith2microservices.models.couplings.SemanticCoupling;
import ch.uzh.ifi.seal.monolith2microservices.models.evaluation.TreeSitterGranularity;
import ch.uzh.ifi.seal.monolith2microservices.models.git.GitRepository;
import ch.uzh.ifi.seal.monolith2microservices.services.decomposition.semanticcoupling.classprocessing.TreeSitterClassContentVisitor;
import ch.uzh.ifi.seal.monolith2microservices.services.decomposition.semanticcoupling.tfidf.TfIdfWrapper;

@Service
public class TreeSitterSemanticCouplingEngine {

    private static Logger logger = LoggerFactory.getLogger(TreeSitterSemanticCouplingEngine.class);

    @Autowired
    private Configs config;

    public List<SemanticCoupling> computeCouplings(GitRepository repo, TreeSitterGranularity granularity)
            throws IOException {
        List<SemanticCoupling> couplings = new ArrayList<>();

        var localRepoPath = config.localRepositoryDirectory + "/" + repo.getName() + "_" + repo.getId();

        var repoDir = Paths.get(localRepoPath);

        var visitor = new TreeSitterClassContentVisitor(repo, granularity);
        Files.walkFileTree(repoDir, visitor);
        var contents = visitor.getContents();
        for (var current : contents) {
            for (var other : contents) {
                if (!current.equals(other)) {
                    var coupling = new SemanticCoupling(current.getQualifiedName(), other.getQualifiedName(),
                            TfIdfWrapper.computeSimilarity(
                                    List.of(current.getRawContents().split("\n")),
                                    List.of(other.getRawContents().split("\n"))));

                    couplings.add(coupling);
                }
            }
        }
        return couplings;
    }
}
