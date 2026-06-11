package io.github.jmecn.ftbquestexport.pojo;

/** Mutable accumulator while writing exported asset files. */
public final class AssetExportCounters {

    public int files;
    public long bytes;
    public int failures;
    public int written;
}
