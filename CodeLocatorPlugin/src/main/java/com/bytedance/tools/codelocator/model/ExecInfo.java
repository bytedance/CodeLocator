package com.bytedance.tools.codelocator.model;

import java.util.Objects;

public class ExecInfo {

    public String version;

    public long execTime;

    public ExecInfo() {

    }

    public ExecInfo(String version, long execTime) {
        this.version = version;
        this.execTime = execTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExecInfo execInfo = (ExecInfo) o;
        return Objects.equals(version, execInfo.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }
}
