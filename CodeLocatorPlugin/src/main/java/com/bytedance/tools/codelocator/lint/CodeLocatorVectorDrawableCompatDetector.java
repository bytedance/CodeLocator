package com.bytedance.tools.codelocator.lint;

import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.util.PathString;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.resources.ResourceUrl;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.detector.api.*;
import com.android.utils.XmlUtils;
import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig;
import com.bytedance.tools.codelocator.utils.Log;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;

public class CodeLocatorVectorDrawableCompatDetector extends ResourceXmlDetector {

    public static final Issue ISSUE;

    private boolean mSkipChecks;

    private final Set<String> mVectors = Sets.newHashSet();

    private boolean mUseSupportLibrary = false;

    public void beforeCheckRootProject(Context context) {
        try {
            this.mSkipChecks = false;
            final Project project = context.getProject();
            mUseSupportLibrary = CodeLocatorUserConfig.loadConfig().getSupportLib(CodeLocatorApiDetector.getModulePath(project));
            hookBeforeDector(context);
        } catch (Throwable t) {
        }
    }

    private void hookBeforeDector(Context context) {
        try {
            final LintDriver driver = context.getDriver();
            final Field applicableDetectors = CodeLocatorApiDetector.getClassField(LintDriver.class, "applicableDetectors");
            applicableDetectors.setAccessible(true);

            final List<Detector> list = (List<Detector>) applicableDetectors.get(driver);
            replaceListItem(list);

            final Field scopeDetectorsFiled = CodeLocatorApiDetector.getClassField(LintDriver.class, "scopeDetectors");
            scopeDetectorsFiled.setAccessible(true);
            Map<Scope, List<Detector>> scopeDetectors = (Map<Scope, List<Detector>>) scopeDetectorsFiled.get(driver);
            final List<Detector> resourceDetectors = scopeDetectors.get(Scope.RESOURCE_FILE);
            replaceListItem(resourceDetectors);

            final List<Detector> allResDetectors = scopeDetectors.get(Scope.ALL_RESOURCE_FILES);
            replaceListItem(allResDetectors);
        } catch (Throwable t) {
            Log.e("hookBeforeDector error", t);
        }
    }

    private void replaceListItem(List<Detector> list) {
        int j = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getClass().equals(getClass())) {
                j = i;
                break;
            }
        }
        for (int i = 0; i < list.size(); i++) {
            if ("com.android.tools.lint.checks.VectorDrawableCompatDetector".equals(list.get(i).getClass().getName())) {
                list.set(i, list.get(j));
                break;
            }
        }
    }

    @Override
    public void beforeCheckFile(@NotNull Context context) {
    }

    @Override
    public void visitDocument(@NotNull XmlContext context, @NotNull Document document) {
    }

    public boolean appliesTo(ResourceFolderType folderType) {
        if (this.mSkipChecks) {
            return false;
        } else {
            return folderType == ResourceFolderType.DRAWABLE || folderType == ResourceFolderType.LAYOUT;
        }
    }

    public void visitElement(XmlContext context, Element element) {
        if (!this.mSkipChecks) {
            String resourceName = Lint.getBaseName(context.file.getName());
            this.mVectors.add(resourceName);
        }
    }

    public Collection<String> getApplicableElements() {
        return this.mSkipChecks ? null : Arrays.asList("vector", "animated-vector");
    }

    public Collection<String> getApplicableAttributes() {
        return this.mSkipChecks ? null : ImmutableList.of("src", "srcCompat");
    }

    public void visitAttribute(XmlContext context, Attr attribute) {
        if (!this.mSkipChecks) {
            boolean incrementalMode = !context.getDriver().getScope().contains(Scope.ALL_RESOURCE_FILES);
            if (incrementalMode || !this.mVectors.isEmpty()) {
                Predicate isVector;
                if (!incrementalMode) {
                    Set var10000 = this.mVectors;
                    isVector = var10000::contains;
                } else {
                    LintClient client = context.getClient();
                    ResourceRepository resources = client.getResourceRepository(context.getMainProject(), true, false);
                    if (resources == null) {
                        return;
                    }

                    isVector = (namex) -> {
                        return checkResourceRepository(resources, (String) namex);
                    };
                }

                String name = attribute.getLocalName();
                String namespace = attribute.getNamespaceURI();
                if ((!"src".equals(name) || "http://schemas.android.com/apk/res/android".equals(namespace)) && (!"srcCompat".equals(name) || "http://schemas.android.com/apk/res-auto".equals(namespace))) {
                    ResourceUrl resourceUrl = ResourceUrl.parse(attribute.getValue());
                    if (resourceUrl != null) {
                        Location location;
                        String message;
                        if (this.mUseSupportLibrary && "src".equals(name) && isVector.test(resourceUrl.name)) {
                            location = context.getNameLocation(attribute);
                            message = "When using VectorDrawableCompat, you need to use `app:srcCompat`.";
                            context.report(ISSUE, attribute, location, message);
                        }

                        if (!this.mUseSupportLibrary && "srcCompat".equals(name) && isVector.test(resourceUrl.name)) {
                            location = context.getNameLocation(attribute);
                            message = "To use VectorDrawableCompat, you need to set `android.defaultConfig.vectorDrawables.useSupportLibrary = true`.";
                            context.report(ISSUE, attribute, location, message);
                        }

                    }
                }
            }
        }
    }

    private static boolean checkResourceRepository(ResourceRepository resources, String name) {
        List<ResourceItem> items = resources.getResources(ResourceNamespace.TODO(), ResourceType.DRAWABLE, name);
        Iterator var3 = items.iterator();

        PathString source;
        File file;
        do {
            if (!var3.hasNext()) {
                return false;
            }

            ResourceItem item = (ResourceItem) var3.next();
            source = item.getSource();
            if (source == null) {
                return false;
            }

            file = source.toFile();
            if (file == null) {
                return false;
            }
        } while (!source.getFileName().endsWith(".xml"));

        String rootTagName = XmlUtils.getRootTagName(file);
        return "vector".equals(rootTagName) || "animated-vector".equals(rootTagName);
    }

    static {
        ISSUE = Issue.create("CodeLocatorVectorDrawableCompat",
            "Using VectorDrawableCompat",
            "To use VectorDrawableCompat, you need to make two modifications to your project. First, set `android.defaultConfig.vectorDrawables.useSupportLibrary = true` in your `build.gradle` file, and second, use `app:srcCompat` instead of `android:src` to refer to vector drawables.",
            Category.CORRECTNESS,
            5,
            Severity.ERROR,
            new Implementation(CodeLocatorVectorDrawableCompatDetector.class,
                Scope.ALL_RESOURCES_SCOPE,
                new EnumSet[]{Scope.RESOURCE_FILE_SCOPE})).addMoreInfo("http://chris.banes.me/2016/02/25/appcompat-vector/#enabling-the-flag");
    }
}