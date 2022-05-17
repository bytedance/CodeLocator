package com.bytedance.tools.codelocator.lint;

import com.android.sdklib.AndroidVersion;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.*;
import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig;
import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CodeLocatorApiDetector extends Detector implements XmlScanner, SourceCodeScanner, ResourceFolderScanner {

    @Nullable
    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        List<Class<? extends UElement>> types = new ArrayList(12);
        types.add(USimpleNameReferenceExpression.class);
        types.add(ULocalVariable.class);
        types.add(UTryExpression.class);
        types.add(UBinaryExpressionWithType.class);
        types.add(UBinaryExpression.class);
        types.add(UCallExpression.class);
        types.add(UClass.class);
        types.add(UMethod.class);
        types.add(UForEachExpression.class);
        types.add(UClassLiteralExpression.class);
        types.add(USwitchExpression.class);
        types.add(UCallableReferenceExpression.class);
        return types;
    }

    private static HashMap<String, String> modulePathCache = new HashMap<>();

    public static String getModulePath(Project project) {
        try {
            if (modulePathCache.containsKey(project.getDir().getAbsolutePath())) {
                return modulePathCache.get(project.getDir().getAbsolutePath());
            }
            final LintClient client = project.getClient();
            final Field myProject = getClassField(client.getClass(), "myProject");
            myProject.setAccessible(true);
            final com.intellij.openapi.project.Project ideProject = (com.intellij.openapi.project.Project) myProject.get(client);
            final String basePath = FileUtils.getProjectFilePath(ideProject);
            modulePathCache.put(project.getDir().getAbsolutePath(), basePath);
            return basePath;
        } catch (Throwable e) {
            Log.e("Change MinSDK Issue Error", e);
        }
        return null;
    }

    private static HashMap<String, Field> sCacheMap = new HashMap<>();

    private boolean isCodeFile(Context context) {
        try {
            final LintDriver driver = context.getDriver();
            final Field applicableDetectors = getClassField(LintDriver.class, "applicableDetectors");
            applicableDetectors.setAccessible(true);
            final List<Detector> list = (List<Detector>) applicableDetectors.get(driver);
            for (int i = 0; i < list.size(); i++) {
                if ("com.android.tools.lint.checks.VectorDrawableCompatDetector".equals(list.get(i).getClass().getName())) {
                    return false;
                }
            }
        } catch (Throwable t) {
            Log.e("isCodeFile error", t);
        }
        return true;
    }

    @Override
    public void visitDocument(@NotNull XmlContext context, @NotNull Document document) {
    }

    @Override
    public void visitAttribute(@NotNull XmlContext context, @NotNull Attr attribute) {
    }

    @Override
    public void visitElement(@NotNull XmlContext context, @NotNull Element element) {
    }

    private void hookBeforeDector(Context context) {
        try {
            final LintDriver driver = context.getDriver();
            final Field applicableDetectors = getClassField(LintDriver.class, "applicableDetectors");
            applicableDetectors.setAccessible(true);
            final List<Detector> list = (List<Detector>) applicableDetectors.get(driver);
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getClass().getName().equals(getClass().getName())) {
                    break;
                } else {
                    list.get(i).beforeCheckRootProject(context);
                    list.get(i).beforeCheckEachProject(context);
                }
            }
            for (int i = 0; i < list.size(); i++) {
                if ("com.android.tools.lint.checks.VectorDrawableCompatDetector".equals(list.get(i).getClass().getName())) {
                    try {
                        list.get(i).beforeCheckRootProject(context);
                        list.get(i).beforeCheckEachProject(context);
                        String cacheKey = list.get(i).getClass() + ".mSkipChecks";
                        Field mSkipChecks = null;
                        if (sCacheMap.containsKey(cacheKey)) {
                            mSkipChecks = sCacheMap.get(cacheKey);
                        } else {
                            mSkipChecks = getClassField(list.get(i).getClass(), "mSkipChecks");
                            sCacheMap.put(cacheKey, mSkipChecks);
                        }
                        if (mSkipChecks != null) {
                            mSkipChecks.set(list.get(i), false);
                        } else {
                            String skipChecksCacheKey = list.get(i).getClass() + ".skipChecks";
                            Field skipChecks = null;
                            if (sCacheMap.containsKey(skipChecksCacheKey)) {
                                skipChecks = sCacheMap.get(skipChecksCacheKey);
                            } else {
                                skipChecks = getClassField(list.get(i).getClass(), "skipChecks");
                                sCacheMap.put(cacheKey, skipChecks);
                            }
                            if (skipChecks != null) {
                                skipChecks.set(list.get(i), false);
                            }
                        }
                    } catch (Throwable t) {
                        Log.e("hookBeforeDector error: " + t);
                    }
                    break;
                }
            }
        } catch (Throwable t) {
            Log.e("hookBeforeDector error", t);
        }
    }

    private void hookProjectMinSdk(Context context, Project project, int minSDK) {
        try {
            Field manifestMinSdk = null;
            String cacheKey = project.getClass().getName() + ".manifestMinSdk";
            if (sCacheMap.containsKey(cacheKey)) {
                manifestMinSdk = sCacheMap.get(cacheKey);
            } else {
                manifestMinSdk = getClassField(project.getClass(), "manifestMinSdk");
                sCacheMap.put(cacheKey, manifestMinSdk);
            }
            if (manifestMinSdk != null) {
                manifestMinSdk.set(project, new AndroidVersion(minSDK, null));
            }
        } catch (Throwable e) {
            Log.e("Change MinSDK Issue Error", e);
        }
        try {
            Field myMinSdkVersion = null;
            String minCacheKey = project.getClass().getName() + ".myMinSdkVersion";
            if (sCacheMap.containsKey(minCacheKey)) {
                myMinSdkVersion = sCacheMap.get(minCacheKey);
            } else {
                myMinSdkVersion = getClassField(project.getClass(), "myMinSdkVersion");
                sCacheMap.put(minCacheKey, myMinSdkVersion);
            }
            if (myMinSdkVersion != null) {
                myMinSdkVersion.set(project, new AndroidVersion(minSDK, null));
            }
        } catch (Throwable e) {
        }
    }

    @Override
    public void beforeCheckRootProject(@NotNull Context context) {
        try {
            final boolean isCodeFile = isCodeFile(context);
            if (isCodeFile) {
                return;
            }
            final Project project = context.getProject();
            final int minSdk = CodeLocatorUserConfig.loadConfig().getMinSdk(getModulePath(project));
            if (minSdk <= 0) {
                return;
            }
            hookProjectMinSdk(context, project, minSdk);
            hookBeforeDector(context);
        } catch (Throwable e) {
            Log.d("Call others error", e);
        }
    }

    @Nullable
    @Override
    public UElementHandler createUastHandler(@NotNull JavaContext context) {
        final Project project = context.getProject();
        final int minSdk = CodeLocatorUserConfig.loadConfig().getMinSdk(getModulePath(project));
        if (minSdk <= 0) {
            return null;
        }
        hookProjectMinSdk(context, context.getProject(), minSdk);
        return null;
    }

    public static Field getClassField(Class clz, String fieldName) {
        while (clz != null && clz != Object.class) {
            try {
                final Field declaredField = clz.getDeclaredField(fieldName);
                declaredField.setAccessible(true);
                return declaredField;
            } catch (NoSuchFieldException ignore) {
            }
            clz = clz.getSuperclass();
        }
        return null;
    }

}
