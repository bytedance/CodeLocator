package com.bytedance.tools.codelocator.model;

import java.util.Collection;

public class DependenciesInfo {

    private Collection<Dependencies> dependenciesList;

    private String mode;

    public Collection<Dependencies> getDependenciesList() {
        return dependenciesList;
    }

    public void setDependencies(Collection<Dependencies> dependenciesList) {
        this.dependenciesList = dependenciesList;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

}
