package ch.uzh.ifi.seal.monolith2microservices.services.decomposition.semanticcoupling.classprocessing;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.treesitter.TSTree;

import ch.uzh.ifi.seal.monolith2microservices.models.TreeSitterContents;
import ch.uzh.ifi.seal.monolith2microservices.models.evaluation.TreeSitterGranularity;
import ch.uzh.ifi.seal.monolith2microservices.models.git.GitRepository;
import ch.uzh.ifi.seal.monolith2microservices.utils.TreeSitter;

public class TreeSitterClassContentVisitor extends SimpleFileVisitor<Path> {

    private static Logger logger = LoggerFactory.getLogger(TreeSitterClassContentVisitor.class);

    private PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.{java,py,rb}");
    private List<TreeSitterContents> contents;
    private TreeSitterGranularity granularity;

    public TreeSitterClassContentVisitor(GitRepository repo, TreeSitterGranularity granularity) {
        this.contents = new ArrayList<>();
        this.granularity = granularity;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        var name = path.getFileName();
        if (matcher.matches(name)) {
            var parser = TreeSitter.getLangFromFilename(name.toString()).map(TreeSitter::getParser);
            logger.info("Parser Exists: " + parser.isPresent());
            parser.ifPresent(p -> {
                TSTree tree;
                try {
                    logger.info("Parsing " + path.toString());
                    tree = p.parseString(null, Files.readString(path));
                    var matches = TreeSitter.getMatches(tree, p, granularity, path)
                            .collect(Collectors.toList());
                    logger.info("# of Matches: " + matches.size());
                    contents.addAll(matches);
                } catch (IOException e) {
                    logger.error(e.toString());
                }
            });
        }
        return FileVisitResult.CONTINUE;
    }

    public List<TreeSitterContents> getContents() {
        return this.contents;
    }
}
