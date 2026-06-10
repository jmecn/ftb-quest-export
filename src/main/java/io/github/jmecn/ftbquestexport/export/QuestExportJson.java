package io.github.jmecn.ftbquestexport.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Shared pretty-printing Gson for export JSON artifacts. */
public final class QuestExportJson {

    public static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();

    private QuestExportJson() {}
}
