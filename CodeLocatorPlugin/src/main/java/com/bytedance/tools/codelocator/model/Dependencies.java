package com.bytedance.tools.codelocator.model;

import java.util.Objects;

public class Dependencies {

    private String artifact;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependencies that = (Dependencies) o;
        return Objects.equals(artifact, that.artifact) &&
                Objects.equals(group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact, group);
    }

    private String group;

    private String version;

    private boolean isDebugDepend;

    public boolean isDebugDepend() {
        return isDebugDepend;
    }

    public void setDebugDepend(boolean debugDepend) {
        isDebugDepend = debugDepend;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return group + ":" + artifact + ":" + version;
    }
}
