package com.bytedance.tools.codelocator.lint;

import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.detector.api.*;
import com.bytedance.tools.codelocator.utils.IdeaUtils;
import com.bytedance.tools.codelocator.utils.Log;
import com.intellij.codeInspection.GlobalInspectionTool;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class CodeLocatorAndroidLintNewApiInspection extends GlobalInspectionTool {

    protected final Issue myIssue;

    private final String myDisplayName;

    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "Android Lint: Correctness";
    }

    @Override
    public String getDisplayName() {
        return myDisplayName;
    }

    public CodeLocatorAndroidLintNewApiInspection(String displayName, Issue issue) {
        myDisplayName = displayName;
        myIssue = issue;
    }

    public CodeLocatorAndroidLintNewApiInspection() {
        this("CodeLocatorAndroidLintNewApiInspection", Issue.create("NewApiCodeLocator",
            "Calling new methods on older versions",
            "This check scans through all the Android API calls in the application and warns about any calls that are not available on **all** versions targeted by this application (according to its minimum SDK attribute in the manifest).\n\nIf you really want to use this API and don't need to support older devices just set the `minSdkVersion` in your `build.gradle` or `AndroidManifest.xml` files.\n\nIf your code is **deliberately** accessing newer APIs, and you have ensured (e.g. with conditional execution) that this code will only ever be called on a supported platform, then you can annotate your class or method with the `@TargetApi` annotation specifying the local minimum SDK to apply, such as `@TargetApi(11)`, such that this check considers 11 rather than your manifest file's minimum SDK as the required API level.\n\nIf you are deliberately setting `android:` attributes in style definitions, make sure you place this in a `values-v`*NN* folder in order to avoid running into runtime conflicts on certain devices where manufacturers have added custom attributes whose ids conflict with the new ones on later platforms.\n\nSimilarly, you can use tools:targetApi=\"11\" in an XML file to indicate that the element will only be inflated in an adequate context.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(CodeLocatorApiDetector.class,
                EnumSet.of(Scope.JAVA_FILE, Scope.RESOURCE_FILE, Scope.MANIFEST),
                new EnumSet[]{Scope.JAVA_FILE_SCOPE, Scope.RESOURCE_FILE_SCOPE, Scope.MANIFEST_SCOPE})).setAndroidSpecific(true));
        addIssue(myIssue);

        if (IdeaUtils.getVersionStr() != null && IdeaUtils.getVersionInt() >= 2021001001) {
            addIssue(CodeLocatorVectorDrawableCompatDetector.ISSUE);
        }
    }

    private void addIssue(Issue issue) {
        Field sIssues = null;
        try {
            new BuiltinIssueRegistry();
            sIssues = BuiltinIssueRegistry.class.getDeclaredField("sIssues");
        } catch (Throwable t) {
            try {
                sIssues = BuiltinIssueRegistry.class.getDeclaredField("builtinIssues");
            } catch (NoSuchFieldException e) {
                Log.e("Add Issue Error", t);
            }
        }
        if (sIssues == null) {
            Log.d("Issue null");
            return;
        }
        try {
            sIssues.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(sIssues, sIssues.getModifiers() & ~Modifier.FINAL);
            final List<Issue> issueList = (List<Issue>) sIssues.get(null);
            if (issueList.size() > 1
                && !(issueList.get(issueList.size() - 1).getId().contains(issue.getId())
                || issueList.get(issueList.size() - 2).getId().contains(issue.getId()))) {
                final ArrayList arrayList = new ArrayList(issueList);
                arrayList.add(issue);
                sIssues.set(null, arrayList);
            }
        } catch (Throwable e) {
            Log.e("Add Issue Error", e);
        }
        Log.d("Add Issue Success");
    }

}
