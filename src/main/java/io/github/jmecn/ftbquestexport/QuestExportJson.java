package io.github.jmecn.ftbquestexport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class QuestExportJson {

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private QuestExportJson() {}
}
