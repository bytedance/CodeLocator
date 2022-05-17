package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SchemaInfo implements Serializable {

    public SchemaInfo(String schema) {
        this(schema, null);
    }

    public SchemaInfo(String schema, String desc) {
        this.mSchema = schema;
        this.mDesc = desc;
        if (mSchema == null) {
            throw new IllegalArgumentException("Schema can't be null");
        }
    }

    /**
     * 对应的Schema全路径
     */
    @SerializedName("db")
    private String mSchema;

    /**
     * Schema描述
     */
    @SerializedName("dc")
    private String mDesc;

    public String getSchema() {
        return mSchema;
    }

    public String getDesc() {
        return mDesc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaInfo that = (SchemaInfo) o;
        return CodeLocatorUtils.equals(mSchema, that.mSchema);
    }

    @Override
    public int hashCode() {
        return CodeLocatorUtils.hash(mSchema);
    }
}