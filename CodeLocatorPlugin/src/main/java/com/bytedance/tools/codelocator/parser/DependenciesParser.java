package com.bytedance.tools.codelocator.parser;

import com.bytedance.tools.codelocator.model.Dependencies;
import com.bytedance.tools.codelocator.model.DependenciesInfo;
import com.bytedance.tools.codelocator.utils.Log;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

public class DependenciesParser {

    public interface TYPE {

        String DebugCompileClassPath = "DebugCompileClassPath";

        String DebugRuntimeClassPath = "DebugRuntimeClassPath";

        String ReleaseCompileClasspath = "ReleaseCompileClasspath";

        String ReleaseRuntimeClassPath = "ReleaseRuntimeClassPath";
    }

    public static final String[] sNeedFilterRepo = {
        "androidx.",
        "android.arch",
        "com.google.",
        "com.android.support",
        "org.jetbrains"
    };

    private String mDependenciesInfo;

    public DependenciesParser(String info) {
        if (info == null) {
            return;
        }
        mDependenciesInfo = info;
    }

    private Collection<Dependencies> getFilterDependencies(Collection<Dependencies> dependencies) {
        if (dependencies == null) {
            return null;
        }
        final Iterator<Dependencies> iterator = dependencies.iterator();
        while (iterator.hasNext()) {
            final Dependencies next = iterator.next();
            for (String filterRepo : sNeedFilterRepo) {
                if (next.getGroup().startsWith(filterRepo)) {
                    iterator.remove();
                    break;
                }
            }
        }
        return dependencies;
    }

    public List<DependenciesInfo> parser() {
        if (mDependenciesInfo == null) {
            return null;
        }
        List<DependenciesInfo> dependenciesInfos = getDependenciesInfos();
        if (dependenciesInfos.isEmpty()) {
            dependenciesInfos = getDependenciesInfos();
            if (dependenciesInfos.isEmpty()) {
                return null;
            }
        }
        return dependenciesInfos;
    }

    @Nullable
    private List<DependenciesInfo> getDependenciesInfos() {
        String debugCompileClass = "debugCompileClassPath".toLowerCase();
        String debugRuntimeClass = "debugRuntimeClassPath".toLowerCase();
        String releaseCompileClass = "releaseCompileClasspath".toLowerCase();
        String releaseRuntimeClass = "releaseRuntimeClassPath".toLowerCase();
        List<DependenciesInfo> dependenciesInfos = new LinkedList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(mDependenciesInfo));
            String currentLine = null;
            while ((currentLine = bufferedReader.readLine()) != null) {
                final String currentLineLower = currentLine.toLowerCase();
                if (currentLineLower.contains(debugCompileClass)) {
                    DependenciesInfo info = getDependenciesByMode(bufferedReader, TYPE.DebugCompileClassPath);
                    dependenciesInfos.add(info);
                } else if (currentLineLower.contains(debugRuntimeClass)) {
                    DependenciesInfo info = getDependenciesByMode(bufferedReader, TYPE.DebugRuntimeClassPath);
                    dependenciesInfos.add(info);
                } else if (currentLineLower.contains(releaseCompileClass)) {
                    DependenciesInfo info = getDependenciesByMode(bufferedReader, TYPE.ReleaseCompileClasspath);
                    dependenciesInfos.add(info);
                } else if (currentLineLower.contains(releaseRuntimeClass)) {
                    DependenciesInfo info = getDependenciesByMode(bufferedReader, TYPE.ReleaseRuntimeClassPath);
                    dependenciesInfos.add(info);
                }
            }
        } catch (Throwable t) {
            Log.e("getDependenciesInfos error", t);
        }
        return dependenciesInfos;
    }

    private DependenciesInfo getDependenciesByMode(BufferedReader bufferedReader, String mode) {
        DependenciesInfo info = new DependenciesInfo();
        info.setMode(mode);
        final Collection<Dependencies> testCompileClasspath = getTestCompileClasspath(bufferedReader);
        final Collection<Dependencies> filterDependencies = getFilterDependencies(testCompileClasspath);
        info.setDependencies(filterDependencies);
        return info;
    }

    private Collection<Dependencies> getTestCompileClasspath(BufferedReader reader) {
        Set<Dependencies> dependenciesSet = new HashSet<>();
        try {
            String currentLine = null;
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.startsWith("+") || currentLine.startsWith("|")) {
                    final Dependencies dependencies = getDependencies(currentLine);
                    if (dependencies != null) {
                        dependenciesSet.add(dependencies);
                    }
                } else if (currentLine.startsWith("\\")) {
                    final Dependencies dependencies = getDependencies(currentLine);
                    if (dependencies != null) {
                        dependenciesSet.add(dependencies);
                    }
                    break;
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dependenciesSet;
    }

    private Dependencies getDependencies(String line) {
        final String depStart = "---";
        final String changeVersionStart = "->";
        final int startIndex = line.indexOf(depStart);
        if (startIndex < 0) {
            return null;
        }
        final int contentIndex = startIndex + depStart.length();
        final int versionChangeIndex = line.indexOf(changeVersionStart, startIndex);
        if (versionChangeIndex > -1) {
            return getDependencies(line.substring(contentIndex, versionChangeIndex).trim(), line.substring(versionChangeIndex + changeVersionStart.length()).trim());
        } else {
            return getDependencies(line.substring(contentIndex).trim(), null);
        }
    }

    public static Dependencies getDependencies(String dependenciesStr, String changedVersionStr) {
        if (changedVersionStr != null && changedVersionStr.contains(":")) {
            return null;
        }
        dependenciesStr = dependenciesStr.replace("(*)", "");
        final String[] split = dependenciesStr.split(":");
        if (split.length != 3) {
            return null;
        }
        Dependencies dependencies = new Dependencies();
        dependencies.setGroup(split[0].trim());
        dependencies.setArtifact(split[1].trim());
        String dependenciesVersion = getDependenciesVersion(split[2].trim(), changedVersionStr);
        dependencies.setVersion(dependenciesVersion);
        return dependencies;
    }

    private static String getDependenciesVersion(String dependenciesVersionStr, String changedVersionStr) {
        if (changedVersionStr != null) {
            dependenciesVersionStr = changedVersionStr;
        }
        dependenciesVersionStr = dependenciesVersionStr.replace("(*)", "")
            .replace("(c)", "").trim();
        final int lastIndex = dependenciesVersionStr.lastIndexOf(":");
        if (lastIndex > -1) {
            dependenciesVersionStr = dependenciesVersionStr.substring(lastIndex + 1).trim();
        }
        if (dependenciesVersionStr != null && dependenciesVersionStr.startsWith("{") && dependenciesVersionStr.endsWith("}")) {
            final String[] splitVersion = dependenciesVersionStr.replace("{", "").replace("}", "").trim().split(" ");
            if (splitVersion.length > 1) {
                return splitVersion[1];
            }
        }
        return dependenciesVersionStr;
    }

}
