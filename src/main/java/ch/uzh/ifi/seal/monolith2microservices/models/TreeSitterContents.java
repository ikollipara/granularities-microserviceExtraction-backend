package ch.uzh.ifi.seal.monolith2microservices.models;

import org.treesitter.TSNode;

public class TreeSitterContents implements Comparable<TreeSitterContents> {
    private String name;
    private String filename;
    private String rawContents;
    private TSNode node;

    public TreeSitterContents(String name, String filename, String rawContents, TSNode node) {
        this.name = name;
        this.filename = filename;
        this.rawContents = rawContents;
        this.node = node;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public String getRawContents() {
        return rawContents;
    }

    public TSNode getNode() {
        return node;
    }

    public String getQualifiedName() {
        return getFilename() + "::" + getName();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        var o = (TreeSitterContents) other;
        return o.getName().equals(name) && o.getFilename().equals(filename);
    }

    @Override
    public int hashCode() {
        return (name + filename).hashCode();
    }

    @Override
    public int compareTo(TreeSitterContents o) {
        return getQualifiedName().compareTo(o.getQualifiedName());
    }
}
