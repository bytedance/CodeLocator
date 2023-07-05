package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.dialog.ShowSearchDialog;
import com.bytedance.tools.codelocator.model.JumpInfo;
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.util.gotoByName.GotoClassModel2;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.FilenameIndex;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.util.TextUtils;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtClass;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdeaUtils {

    private static Long sVersionNum;

    private static String sVersionStr = null;

    private static Pattern sFlavorPattern = Pattern.compile("[a-z0-9]+");

    @Nullable
    private static String getCurrentBuildVariant(Project project) {
        try {
            final Module[] modules = ModuleManager.getInstance(project).getModules();
            for (Module m : modules) {
                final AndroidFacet instance = AndroidFacet.getInstance(m);
                if (instance == null) {
                    continue;
                }
                final Method getPropertiesMethod = ReflectUtils.getClassMethod(instance.getClass(), "getProperties");
                final Object properties = getPropertiesMethod.invoke(instance);
                if (properties == null) {
                    continue;
                }
                final Field selectedBuildVariantField = ReflectUtils.getClassField(properties.getClass(),
                    "SELECTED_BUILD_VARIANT");
                final String selectedBuildVariant = (String) selectedBuildVariantField.get(properties);
                if (selectedBuildVariant == null || selectedBuildVariant.isEmpty()) {
                    continue;
                }
                final Field projectTypeField = ReflectUtils.getClassField(properties.getClass(), "PROJECT_TYPE");
                boolean isAppModule = false;
                if (projectTypeField != null) {
                    final int type = (int) projectTypeField.get(properties);
                    isAppModule = (type == 0);
                }
                final Field isLibraryField = ReflectUtils.getClassField(properties.getClass(), "LIBRARY_PROJECT");
                if (isLibraryField != null) {
                    final boolean isLibrary = (boolean) isLibraryField.get(properties);
                    isAppModule = !isLibrary;
                }
                if (isAppModule) {
                    final Matcher matcher = sFlavorPattern.matcher(selectedBuildVariant);
                    if (matcher.find()) {
                        String flavor = matcher.group();
                        Log.d("AppModule " + m.getName() + ", flavor: " + flavor);
                        if (flavor.equals("debug") || flavor.equals("release")) {
                            return "";
                        } else {
                            return flavor;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.e("getCurrentBuildVariant error", t);
        }
        return "";
    }

    public static void logVersion() {
        Log.d("getApiVersion: " + ApplicationInfo.getInstance().getApiVersion()
            + ", getBuild: " + ApplicationInfo.getInstance().getBuild()
            + ", getFullVersion: " + ApplicationInfo.getInstance().getFullVersion()
            + ", getVersionName: " + ApplicationInfo.getInstance().getVersionName()
            + ", getCompanyName: " + ApplicationInfo.getInstance().getCompanyName()
            + ", getCompanyURL: " + ApplicationInfo.getInstance().getCompanyURL()
            + ", getMinorVersion: " + ApplicationInfo.getInstance().getMinorVersion()
            + ", getMinorVersion: " + ApplicationInfo.getInstance().getMinorVersion()
            + ", getStrictVersion: " + ApplicationInfo.getInstance().getStrictVersion()
            + ", getVersionInt: " + getVersionInt());
    }

    public static String getVersionStr() {
        if (sVersionStr != null) {
            return sVersionStr;
        }
        sVersionStr = ApplicationInfo.getInstance().getVersionName() + " " + ApplicationInfo.getInstance().getFullVersion();
        return sVersionStr;
    }

    public static long getVersionInt() {
        if (sVersionNum != null) {
            return sVersionNum;
        }
        sVersionNum = StringUtils.getVersionInt(ApplicationInfo.getInstance().getFullVersion());
        return sVersionNum;
    }

    @Nullable
    public static PsiFile findMatchedCodeFile(Project project, String name, String pkgName) {
        ArrayList<PsiFile> matchFiles = new ArrayList<>();
        final String buildVariant = getCurrentBuildVariant(project);
        PsiFile[] findFiles = FilenameIndex.getFilesByName(project, name + ".java", new EverythingGlobalScope(project));
        for (PsiFile f : findFiles) {
            matchFiles.add(f);
        }
        if (matchFiles.size() != 0) {
            final PsiFile selfProjectFile = getSelfProjectFile(buildVariant, matchFiles, pkgName, name);
            if (selfProjectFile != null) {
                return selfProjectFile;
            }
            matchFiles.clear();
        }
        findFiles = FilenameIndex.getFilesByName(project, name + ".kt", new EverythingGlobalScope(project));
        for (PsiFile f : findFiles) {
            if (f.getViewProvider().getVirtualFile().getPresentableUrl().replace(File.separator, ".").contains(pkgName)) {
                matchFiles.add(f);
            }
        }
        if (matchFiles.size() != 0) {
            final PsiFile selfProjectFile = getSelfProjectFile(buildVariant, matchFiles, pkgName, name);
            if (selfProjectFile != null) {
                return selfProjectFile;
            }
            matchFiles.clear();
        }
        findFiles = FilenameIndex.getFilesByName(project, name + ".class", new EverythingGlobalScope(project));
        for (PsiFile f : findFiles) {
            matchFiles.add(f);
        }
        if (matchFiles.size() != 0) {
            final PsiFile selfProjectFile = getSelfProjectFile(buildVariant, matchFiles, pkgName, name);
            if (selfProjectFile != null) {
                return selfProjectFile;
            }
        }
        return null;
    }

    public static PsiFile searchFileByClassName(Project project, String name, boolean withSuffix, String pkName) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        final String buildVariant = getCurrentBuildVariant(project);
        if (withSuffix) {
            PsiFile[] files = FilenameIndex.getFilesByName(project, name, new EverythingGlobalScope(project));
            if (isArrayEmpty(files)) {
                final int i = name.lastIndexOf(".");
                if (i > -1 && !name.endsWith("xml")) {
                    files = FilenameIndex.getFilesByName(project, name.substring(0, i) + ".class",
                        new EverythingGlobalScope(project));
                }
                if (isArrayEmpty(files)) {
                    return null;
                }
            }
            return getSelfProjectFile(buildVariant, Arrays.asList(files), pkName, name);
        } else {
            return findMatchedCodeFile(project, name, pkName);
        }
    }

    private static boolean isArrayEmpty(Object[] objects) {
        return objects == null || objects.length == 0;
    }

    public static boolean goSourceCodeLines(CodeLocatorWindow codeLocatorWindow, Project project, String name, int line,
                                            boolean withSuffix, String pkName, int level) {
        PsiFile psiFile = searchFileByClassName(project, name, withSuffix, pkName);
        if (psiFile != null) {
            int finalLine = line;
            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, psiFile.getVirtualFile(), finalLine, 0);
            Editor editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
            doEditEffect(editor, finalLine);
            return true;
        }
        if (findAndOpenClassFile(project, name, null, line, pkName)) {
            return true;
        }
        alert(codeLocatorWindow, project, name, pkName, "", line, level, null, false);
        return false;
    }

    public static boolean findAndOpenClassFile(Project project, String name, String searchStr, int jumpLine, String pkName) {
        if (name.endsWith(".png") || name.endsWith(".xml") || name.endsWith(".webp")) {
            return false;
        }
        final GotoClassModel2 model = new GotoClassModel2(project);
        if (name.contains(".")) {
            name = name.substring(0, name.lastIndexOf("."));
        }
        final Object[] elementsByName = model.getElementsByName(name, true, name);
        if (elementsByName.length > 0) {
            for (Object ele : elementsByName) {
                if (ele instanceof ClsClassImpl) {
                    if (!(((ClsClassImpl) ele).getQualifiedName().contains(pkName))) {
                        continue;
                    }
                } else if (ele instanceof KtClass) {
                    if (!((KtClass) ele).getFqName().asString().contains(pkName)) {
                        continue;
                    }
                } else {
                    continue;
                }
                if (NavigationUtil.openFileWithPsiElement((PsiElement) ele, true, true)) {
                    if (searchStr != null || jumpLine > -1) {
                        ThreadUtils.runOnUIThread(() -> {
                            if (searchStr == null) {
                                final FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
                                if (selectedEditor instanceof PsiAwareTextEditorImpl) {
                                    final Editor editor = ((PsiAwareTextEditorImpl) selectedEditor).getEditor();
                                    editor.getScrollingModel().scrollTo(new LogicalPosition(jumpLine, 0), ScrollType.CENTER);
                                }
                            } else {
                                final FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
                                if (selectedEditor instanceof PsiAwareTextEditorImpl) {
                                    final Editor editor = ((PsiAwareTextEditorImpl) selectedEditor).getEditor();
                                    Document document = editor.getDocument();
                                    int line = findTextLineInFileContent(searchStr, "", document.getText(), false);
                                    line = line > 0 ? line - 1 : Math.max(line, 0);
                                    editor.getScrollingModel().scrollTo(new LogicalPosition(line, 0), ScrollType.CENTER);
                                    doEditEffect(editor, line, searchStr);
                                }
                            }
                        });
                    }
                    if (((PsiElement) ele).getContainingFile() != null
                        && ((PsiElement) ele).getContainingFile().getVirtualFile().getUrl() != null
                        && ((PsiElement) ele).getContainingFile().getVirtualFile().getUrl().endsWith("class")) {
                        NotificationUtils.showNotifyInfoShort(project, ResUtils.getString("class_file_tip"), 3500L);
                    }
                }
                return true;
            }
        }
        return false;
    }

    /***
     * 无法确定具体行数，根据关键字搜索
     * @param project
     * @param name
     * @return
     */
    public static boolean goSourceCodeAndSearch(CodeLocatorWindow codeLocatorWindow, Project project, String name, String pre,
                                                String searchStr, boolean isMatch, boolean withSuffix, String pkName, int level) {
        PsiFile psiFile = searchFileByClassName(project, name, withSuffix, pkName);
        if (psiFile != null) {
            Log.d("psiFile" + psiFile + ", search " + searchStr);
            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, psiFile.getVirtualFile());
            Editor editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
            Document document = editor.getDocument();
            int line = findTextLineInFileContent(searchStr, pre, document.getText(), isMatch);
            String realSearchStr = searchStr;
            if (line < 0 && searchStr.startsWith("id=\"@+id/")) {
                searchStr = searchStr.substring("id=\"@+id/".length());
                realSearchStr = "id=\"@android:id/" + searchStr + "\"";
                line = findTextLineInFileContent(realSearchStr, pre, document.getText(), isMatch);
                if (line < 0) {
                    if (tryFindIdInXmlInclude(editor, project, document.getText(), searchStr)) {
                        return true;
                    }
                }
            }
            int scrollLine = line > 0 ? line - 1 : Math.max(line, 0);
            editor.getScrollingModel().scrollTo(new LogicalPosition(scrollLine, 0), ScrollType.CENTER);
            doEditEffect(editor, scrollLine, realSearchStr);
            return line > -1;
        }
        if (findAndOpenClassFile(project, name, searchStr, -1, pkName)) {
            return true;
        }
        Log.d("找不到对应文件 name: " + name + ", searchStr: " + searchStr);
        alert(codeLocatorWindow, project, name, pkName, searchStr, -1, level, pre, isMatch);
        return false;
    }

    public static boolean goSourceCodeAndSearch(CodeLocatorWindow codeLocatorWindow, Project project, String name,
                                                String candidateName, String searchStr, String pkName, int level) {
        PsiFile psiFile = null;
        PsiFile candidateFile = null;
        String buildVariant = getCurrentBuildVariant(project);
        PsiFile[] files = FilenameIndex.getFilesByName(project, name + ".java", new EverythingGlobalScope(project));
        if (isArrayEmpty(files)) {
            files = FilenameIndex.getFilesByName(project, name + ".kt", new EverythingGlobalScope(project));
        }
        if (isArrayEmpty(files)) {
            if (candidateName != null && !candidateName.isEmpty()) {
                PsiFile[] candidateFiles = FilenameIndex.getFilesByName(project, candidateName + ".java",
                    new EverythingGlobalScope(project));
                if (isArrayEmpty(candidateFiles)) {
                    candidateFiles = FilenameIndex.getFilesByName(project, candidateName + ".kt",
                        new EverythingGlobalScope(project));
                }
                if (!isArrayEmpty(candidateFiles)) {
                    candidateFile = getSelfProjectFile(buildVariant, Arrays.asList(candidateFiles), pkName, name);
                    if (candidateFile != null) {
                        String searchFileContent = FileUtils.getFileContent(candidateFile.getVirtualFile());
                        if (searchFileContent.contains(searchStr)) {
                            psiFile = candidateFile;
                        }
                    }
                }
            }
            if (psiFile == null) {
                files = FilenameIndex.getFilesByName(project, name + ".class", new EverythingGlobalScope(project));
                if (!isArrayEmpty(files)) {
                    psiFile = getSelfProjectFile(buildVariant, Arrays.asList(files), pkName, name);
                }
            }
        } else {
            psiFile = getSelfProjectFile(buildVariant, Arrays.asList(files), pkName, name);
        }
        if (psiFile != null) {
            Log.d("psiFile" + psiFile + ", search " + searchStr);
            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, psiFile.getVirtualFile());
            Editor editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
            Document document = editor.getDocument();
            int line = findTextLineInFileContent(searchStr, "", document.getText(), false);
            line = line > 0 ? line - 1 : Math.max(line, 0);
            editor.getScrollingModel().scrollTo(new LogicalPosition(line, 0), ScrollType.CENTER);
            doEditEffect(editor, line, searchStr);
            return true;
        }
        if (findAndOpenClassFile(project, name, searchStr, -1, pkName)) {
            return true;
        }
        Log.d("找不到对应文件 name: " + name + ", searchStr: " + searchStr);
        alert(codeLocatorWindow, project, name, pkName, searchStr, -1, level, "", false);
        return false;
    }

    private static boolean tryFindIdInXmlInclude(Editor preEdit, Project project, String fileContent, String searchId) {
        try {
            BufferedReader rdr = new BufferedReader(new StringReader(fileContent));
            HashMap<String, Integer> includeFilesWithLine = new HashMap<>();
            int lineCount = 0;
            for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
                if (line.contains("layout=\"@layout/")) {
                    String[] layouts = line.split("layout=\"@layout/");
                    if (layouts.length > 1) {
                        String layoutFile = layouts[1].trim();
                        final int lastIndex = layoutFile.lastIndexOf("\"");
                        if (lastIndex > -1) {
                            layoutFile = layoutFile.substring(0, lastIndex);
                            includeFilesWithLine.put(layoutFile, lineCount);
                        }
                    }
                }
                lineCount++;
            }
            for (String includeXmlFile : includeFilesWithLine.keySet()) {
                PsiFile psiFile = searchFileByClassName(project, includeXmlFile + ".xml", true, "");
                if (psiFile == null) {
                    continue;
                }
                String layoutContent = FileUtils.getFileContent(psiFile.getVirtualFile());
                if (layoutContent == "") {
                    continue;
                }
                String realSearchStr = "id=\"@+id/" + searchId;
                int line = findTextLineInFileContent(realSearchStr, "", layoutContent, false);
                Log.d("Search Include " + psiFile.getVirtualFile().getName() + ", key: " + realSearchStr + ", line: " + line);
                if (line > -1) {
                    openFileLineAndSelectSearchStr(preEdit, includeXmlFile, includeFilesWithLine.get(includeXmlFile), project,
                        realSearchStr, psiFile, line);
                    return true;
                }
                realSearchStr = "id=\"@android:id/" + searchId;
                line = findTextLineInFileContent(realSearchStr, "", layoutContent, false);
                Log.d("Search Include " + psiFile.getVirtualFile().getName() + ", key: " + realSearchStr + ", line: " + line);
                if (line > -1) {
                    openFileLineAndSelectSearchStr(preEdit, includeXmlFile, includeFilesWithLine.get(includeXmlFile), project,
                        realSearchStr, psiFile, line);
                    return true;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private static void openFileLineAndSelectSearchStr(Editor preEdit, String preFileName, int lineInPreFile, Project project,
                                                       String searchId, PsiFile psiFile, int line) {
        line = Math.max(0, line - 1);
        preEdit.getScrollingModel().scrollTo(new LogicalPosition(lineInPreFile, 0), ScrollType.CENTER);
        doEditEffect(preEdit, lineInPreFile, preFileName);
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, psiFile.getVirtualFile());
        Editor editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
        editor.getScrollingModel().scrollTo(new LogicalPosition(line, 0), ScrollType.CENTER);
        doEditEffect(editor, line, searchId);
    }

    public static void alert(CodeLocatorWindow codeLocatorWindow, Project project, String name, String pkName, String searchStr
        , int line, int level, String pre, boolean isMatch) {
        ThreadUtils.runOnUIThread(() -> new ShowSearchDialog(codeLocatorWindow, project, name, pkName, searchStr, line).showAndGet());
    }

    /***
     * 高亮标注
     */
    private static void doEditEffect(Editor editor, int lines) {
        if (lines <= 1) {
            return;
        }
        try {
            editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(lines, 0));
            editor.getSelectionModel().selectLineAtCaret();
        } catch (Throwable t) {
            Log.e("doEditEffect error", t);
        }
    }

    /***
     * 高亮标注,某段文字
     */
    private static void doEditEffect(Editor editor, int lines, String searchText) {
        if (lines <= 1) {
            return;
        }
        editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(lines, 0));
        editor.getSelectionModel().selectLineAtCaret();
        String lineText = editor.getSelectionModel().getSelectedText();
        int start = editor.getSelectionModel().getSelectionStart();
        int pos = -1;
        if (!TextUtils.isEmpty(lineText)) {
            pos = lineText.indexOf(searchText);
        }
        int offset = 0;
        if (pos != -1) {
            if (searchText.startsWith("class ")) {
                offset = "class ".length();
            }
            editor.getSelectionModel().setSelection(start + pos + offset, start + pos + searchText.length());
        }
        editor.getCaretModel().moveToOffset(start + pos + searchText.length());
    }

    public static int findTextLineInFileContent(String text, String pre, String fileContent, boolean isMatch) {
        int index = 1;
        boolean isMultiLine = false;
        boolean finded = false;
        try {
            BufferedReader rdr = new BufferedReader(new StringReader(fileContent));
            for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
                if (isMatch) {
                    if (isMultiLine) {
                        if (isRealContains(text, line)) {
                            finded = true;
                            break;
                        } else if (line.contains(")")) {
                            isMultiLine = false;
                        }
                    } else if (!pre.isEmpty() && line.contains(pre)) {
                        if (isRealContains(text, line)) {
                            finded = true;
                            break;
                        } else if (!line.contains(")")) {
                            isMultiLine = true;
                        }
                    }
                } else if (isRealContains(text, line)) {
                    finded = true;
                    break;
                }
                index++;
            }
            rdr.close();
            if (!finded) {
                index = -1;
            }
            return index;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static boolean isRealContains(String text, String line) {
        final int textIndex = line.indexOf(text);
        if (textIndex < 0) {
            return false;
        }
        if (line.length() <= textIndex + text.length()) {
            return true;
        }
        final char charAtEnd = line.charAt(textIndex + text.length());
        if ((charAtEnd >= 'a' && charAtEnd <= 'z')
            || (charAtEnd >= 'A' && charAtEnd <= 'Z')
            || (charAtEnd >= '0' && charAtEnd <= '9')
            || (charAtEnd == '_')) {
            return false;
        }
        return true;
    }

    public static boolean navigateByJumpInfo(CodeLocatorWindow codeLocatorWindow, Project project, JumpInfo jumpInfo,
                                          boolean isMatch, String pre, String seachText, boolean withSuffix) {
        if (jumpInfo == null) {
            Log.e("jumpInfo == null");
            return false;
        }
        String pkName = getPackageNameHasSuffix(jumpInfo.getFileName());
        try {
            if (!jumpInfo.needJumpById()) {
                int lineCount = Math.max(jumpInfo.getLineCount() - 1, 0);
                return goSourceCodeLines(codeLocatorWindow, project, jumpInfo.getSimpleFileName(), lineCount, withSuffix, pkName, 0);
            } else {
                final boolean jumpSuccess = goSourceCodeAndSearch(codeLocatorWindow, project, jumpInfo.getSimpleFileName(), pre, seachText, isMatch,
                    withSuffix, pkName, 0);
                if (!jumpSuccess && jumpInfo.isIsViewBinding()) {
                    int lineCount = Math.max(jumpInfo.getLineCount() - 1, 0);
                    return goSourceCodeLines(codeLocatorWindow, project, jumpInfo.getSimpleFileName(), lineCount, withSuffix, pkName, 0);
                }
                return jumpSuccess;
            }
        } catch (Exception e) {
            Log.e("跳转代码失败", e);
        }
        return false;
    }

    public static String getRemovePackageFileName(String fullClassName) {
        if (TextUtils.isEmpty(fullClassName)) {
            return "";
        }
        final int indexOfPkgDot = fullClassName.lastIndexOf(".");
        if (indexOfPkgDot > -1) {
            return fullClassName.substring(indexOfPkgDot + 1);
        }
        return fullClassName;
    }

    public static void openBrowser(Project project, String webSite) throws URISyntaxException, IOException {
        Desktop desktop = Desktop.getDesktop();
        if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
            URI uri = new URI(webSite);
            desktop.browse(uri);
        }
    }

    public static String getPackageNameHasSuffix(String fullClassName) {
        if (fullClassName == null) {
            return "";
        }
        final int indexOfSuffixDot = fullClassName.lastIndexOf(".");
        if (indexOfSuffixDot > -1) {
            final String substring = fullClassName.substring(0, indexOfSuffixDot);
            final int indexOfPkgDot = substring.lastIndexOf(".");
            if (indexOfPkgDot > -1) {
                return substring.substring(0, indexOfPkgDot);
            }
        }
        return "";
    }

    public static String getPackageNameNoSuffix(String fullClassName) {
        final int indexOfDot = fullClassName.lastIndexOf(".");
        if (indexOfDot > -1) {
            return fullClassName.substring(0, indexOfDot);
        }
        return "";
    }

    public static PsiFile getSelfProjectFile(String buildVariant, List<PsiFile> files, String selfPackageName, String fileName) {
        ArrayList<PsiFile> psiFiles = new ArrayList<>();
        for (PsiFile file : files) {
            if (file.getViewProvider().getVirtualFile().getPresentableUrl().replace(File.separator, ".").contains(selfPackageName + "." + fileName)) {
                psiFiles.add(file);
            }
        }

        psiFiles.sort((o1, o2) -> getPsiFileWeight(buildVariant, o2) - getPsiFileWeight(buildVariant, o1));

        PsiFile result = null;
        if (!psiFiles.isEmpty()) {
            result = psiFiles.get(0);
        }
        if (result != null && result.getViewProvider().getVirtualFile().getPresentableUrl().endsWith("class")) {
            NotificationUtils.showNotifyInfoShort(result.getProject(), ResUtils.getString("class_file_tip"), 3500L);
        }
        return result;
    }

    public static int getPsiFileWeight(String buildVariant, PsiFile file) {
        if (file == null) {
            return 0;
        }
        final String filePath = file.getViewProvider().getVirtualFile().getUrl().toLowerCase();

        int addWeight = 0;
        if (buildVariant != null && !buildVariant.isEmpty() && filePath.contains(buildVariant)) {
            addWeight = 1000;
        }
        if (filePath.startsWith("file")) {
            return 100 + addWeight;
        }
        if (filePath.contains(".gradle" + File.separator + "caches")) {
            return 90 + addWeight;
        }
        if (filePath.contains(File.separator + "build" + File.separator)) {
            return 80 + addWeight;
        }
        if (filePath.contains(File.separator + "intermediates" + File.separator)) {
            return 70 + addWeight;
        }
        return addWeight;
    }
}
