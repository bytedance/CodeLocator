package com.bytedance.tools.codelocator.parser;

import com.bytedance.tools.codelocator.model.DisplayDependencies;

import java.util.HashMap;

public class DisplayDependenciesParser {

    private static final String DEPENDENCIES_START = "+---";

    private String[] mDependenciesInfoLines;

    private int mCurrentLine;

    public DisplayDependenciesParser(String info) {
        if (info == null) {
            return;
        }
        mDependenciesInfoLines = info.split("\n");
    }

    private String moveToNextLine() {
        mCurrentLine++;
        return getCurrentLine();
    }

    private String nextLine() {
        if (mCurrentLine + 1 < mDependenciesInfoLines.length) {
            return mDependenciesInfoLines[mCurrentLine + 1];
        }
        return null;
    }


    private String getCurrentLine() {
        if (mCurrentLine < mDependenciesInfoLines.length) {
            return mDependenciesInfoLines[mCurrentLine];
        }
        return null;
    }

    public HashMap<String, DisplayDependencies> parser() {
        if (mDependenciesInfoLines == null) {
            return null;
        }
        String line = getCurrentLine();
        HashMap<String, DisplayDependencies> displayDependenciesHashMap = new HashMap<>();
        while (line != null) {
            if (!isDependenciesStart(line)) {
                line = moveToNextLine();
                continue;
            } else {
                parserDependencies(line, displayDependenciesHashMap);
            }
        }
        return displayDependenciesHashMap;
    }

    private void parserDependencies(String line, HashMap<String, DisplayDependencies> displayDependenciesHashMap) {
        final int indexOfSplit = line.indexOf("-");
        if (indexOfSplit < 0) {
            return;
        }
        final String dependenciesType = line.substring(0, indexOfSplit).trim();
        final DisplayDependencies displayDependencies = parserLine(dependenciesType, 0);
        displayDependenciesHashMap.put(dependenciesType, displayDependencies);
    }

    public DisplayDependencies parserLine(String line, int lineStart) {
        DisplayDependencies dependencies = new DisplayDependencies();
        dependencies.setDependenciesLine(line.substring(lineStart));
        while (true) {
            final String nextLine = nextLine();
            final int nextLineSpaceCount = getLineSpaceCount(nextLine);
            if (nextLine != null && nextLineSpaceCount > lineStart) {
                moveToNextLine();
                DisplayDependencies child = parserLine(nextLine, nextLineSpaceCount);
                dependencies.addChild(child);
            } else {
                break;
            }
        }
        return dependencies;
    }

    private boolean isDependenciesStart(String line) {
        if (line == null) {
            return false;
        }
        if (line.contains("Resolved configuration")) {
            String nextLine = nextLine();
            return nextLine != null && nextLine.startsWith(DEPENDENCIES_START);
        }
        return false;
    }

    public int getLineSpaceCount(String dependenciesLine) {
        if (dependenciesLine == null) {
            return 0;
        }
        int index = 0;
        for (; index < dependenciesLine.length(); index++) {
            if (dependenciesLine.charAt(index) == ' '
                    || dependenciesLine.charAt(index) == '+'
                    || dependenciesLine.charAt(index) == '-'
                    || dependenciesLine.charAt(index) == '\\'
                    || dependenciesLine.charAt(index) == '|') {
                continue;
            } else {
                break;
            }
        }
        if (index < dependenciesLine.length()) {
            return index;
        }
        return 0;
    }
}
