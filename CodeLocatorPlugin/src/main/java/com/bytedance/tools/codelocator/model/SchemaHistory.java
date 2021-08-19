package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.NetUtils;
import com.bytedance.tools.codelocator.utils.ThreadUtils;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class SchemaHistory {

    private static final int MAX_CACHE_HISTORY_COUNT = 100;

    private static SchemaHistory sSchemaHistory;

    @SerializedName("mHistorySchema")
    public List<SchemaInfo> mHistorySchema;

    public List<SchemaInfo> getHistorySchema() {
        return mHistorySchema;
    }

    public void addHistory(SchemaInfo schema) {
        if (this.mHistorySchema == null) {
            mHistorySchema = new LinkedList<>();
        }
        if (mHistorySchema.contains(schema)) {
            mHistorySchema.remove(schema);
            mHistorySchema.add(0, schema);
        } else {
            if (mHistorySchema.size() >= MAX_CACHE_HISTORY_COUNT) {
                for (int i = mHistorySchema.size() - 1; i >= MAX_CACHE_HISTORY_COUNT - 1; i--) {
                    mHistorySchema.remove(i);
                }
            }
            mHistorySchema.add(0, schema);
        }
        updateHistory(this);
    }

    public static @NotNull SchemaHistory loadHistory() {
        if (sSchemaHistory != null) {
            return sSchemaHistory;
        }
        final File configFile = new File(FileUtils.codelocatorMainDir, FileUtils.SCHEMA_FILE_NAME);
        final String fileContent = FileUtils.getFileContent(configFile);
        try {
            if (!fileContent.isEmpty()) {
                final SchemaHistory codelocatorConfig = NetUtils.sGson.fromJson(fileContent, SchemaHistory.class);
                if (codelocatorConfig == null) {
                    configFile.delete();
                } else {
                    sSchemaHistory = codelocatorConfig;
                }
            }
        } catch (Throwable t) {
            configFile.delete();
        }

        if (sSchemaHistory == null) {
            sSchemaHistory = new SchemaHistory();
        }
        return sSchemaHistory;
    }

    public static void updateHistory(SchemaHistory history) {
        sSchemaHistory = history;
        ThreadUtils.submit(() -> {
            final File configFile = new File(FileUtils.codelocatorMainDir, FileUtils.SCHEMA_FILE_NAME);
            FileUtils.saveContentToFile(configFile, NetUtils.sGson.toJson(sSchemaHistory));
        });
    }

}
