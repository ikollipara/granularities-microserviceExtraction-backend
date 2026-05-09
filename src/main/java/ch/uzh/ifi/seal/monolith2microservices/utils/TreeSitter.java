package ch.uzh.ifi.seal.monolith2microservices.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;
import org.treesitter.TreeSitterPython;
import org.treesitter.TreeSitterRuby;

import ch.uzh.ifi.seal.monolith2microservices.models.TreeSitterContents;
import ch.uzh.ifi.seal.monolith2microservices.models.evaluation.TreeSitterGranularity;

public class TreeSitter {

    private static Logger logger = LoggerFactory.getLogger(TreeSitter.class);

    private static Map<String, Map<TreeSitterGranularity, String>> TS_QUERIES = Map.of(
            "py",
            Map.of(
                    TreeSitterGranularity.CLASS,
                    "(class_definition name: (identifier) @class.name body: (block) @class.body) @class",
                    TreeSitterGranularity.FUNCTION,
                    "(function_definition name: (identifier) @function.name body: (block) @function.body) @function",
                    TreeSitterGranularity.MODULE,
                    "(module) @module.body"),
            "rb",
            Map.of(
                    TreeSitterGranularity.CLASS,
                    "(class name: (constant) @class.name body: (body_statement) @class.body) @class"),
            "java",
            Map.of(
                    TreeSitterGranularity.CLASS,
                    "(class_declaration name: (identifier) @class.name body: (class_body)"
                            + " @class.body) @class"));

    public static TSParser getParser(TSLanguage lang) {
        var parser = new TSParser();
        parser.setLanguage(lang);
        return parser;
    }

    public static Stream<TreeSitterContents> getMatches(TSTree tree, TSParser parser, TreeSitterGranularity granularity,
            Path file) throws IOException {
        return getMatches(tree, parser, granularity, Files.readString(file), file.toString());
    }

    public static Stream<TreeSitterContents> getMatches(TSTree tree, TSParser parser,
            TreeSitterGranularity granularity,
            String contents, String fileName) throws IOException {
        List<TreeSitterContents> results = new ArrayList<>();
        var query = getQueryFromParserLang(parser.getLanguage(), granularity);
        var cursor = new TSQueryCursor();
        var match = new TSQueryMatch();
        var lastByteCount = contents.length();
        var contentsAsBytes = contents.getBytes(StandardCharsets.UTF_8);
        // var contents = Files.readString(file);
        if (query.isEmpty())
            logger.error("No Query Present for " + fileName);
        return query.<Stream<TreeSitterContents>>map(q -> {
            cursor.exec(q, tree.getRootNode());
            while (cursor.nextMatch(match)) {
                TSNode nameNode = null;
                TSNode contentsNode = null;

                for (var capture : match.getCaptures()) {
                    switch (q.getCaptureNameForId(capture.getIndex())) {
                        case "class.name" -> nameNode = capture.getNode();
                        case "class.body" -> contentsNode = capture.getNode();
                        case "function.name" -> nameNode = capture.getNode();
                        case "function.body" -> contentsNode = capture.getNode();
                        case "module.body" -> contentsNode = capture.getNode();
                    }
                }
                @SuppressWarnings("null")
                var name = switch (granularity) {
                    case TreeSitterGranularity.MODULE -> Arrays.asList(fileName.split("/")).getLast();
                    default -> new String(contentsAsBytes, nameNode.getStartByte(),
                            nameNode.getEndByte() - nameNode.getStartByte(), StandardCharsets.UTF_8).trim();
                };
                @SuppressWarnings("null")
                var rawContents = new String(contentsAsBytes, contentsNode.getStartByte(),
                        contentsNode.getEndByte() - contentsNode.getStartByte(), StandardCharsets.UTF_8).trim();

                // Ignore the Meta class that is found in Django.
                if (name.equals("Meta"))
                    continue;

                results.add(new TreeSitterContents(name, fileName, rawContents, contentsNode));
            }
            cursor.close();
            return results.stream();

        }).orElse(Stream.empty());

    }

    public static Optional<TSLanguage> getLangFromFilename(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        var lang = (dotIndex > 0 && dotIndex < filename.length() - 1) ? filename.substring(dotIndex + 1) : "";
        return switch (lang) {
            case "py" -> Optional.of(new TreeSitterPython());
            case "java" -> Optional.of(new TreeSitterJava());
            case "rb" -> Optional.of(new TreeSitterRuby());
            default -> Optional.empty();
        };
    }

    private static Optional<TSQuery> getQueryFromParserLang(TSLanguage lang, TreeSitterGranularity granularity) {
        logger.debug("Language Name from Parser: " + lang.name());
        if (lang.name() == null)
            return Optional.empty();
        return switch (lang.name()) {
            case "python" -> Optional.of(new TSQuery(lang, TS_QUERIES.get("py").get(granularity)));
            case "java" -> Optional.empty();
            case "ruby" -> Optional.empty();
            default -> Optional.empty();
        };
    }
}
