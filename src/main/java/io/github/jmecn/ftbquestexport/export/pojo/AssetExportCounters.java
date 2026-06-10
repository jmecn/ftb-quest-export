package io.github.jmecn.ftbquestexport.export.pojo;

/** Mutable accumulator while writing asset closure files. */
public final class AssetExportCounters {

    public int files;
    public long bytes;
    public int failures;
    public int written;
}
