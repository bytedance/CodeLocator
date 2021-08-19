package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.dialog.ShowSearchDialog;
import com.bytedance.tools.codelocator.model.JumpInfo;
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.gotoByName.GotoClassModel2;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.FilenameIndex;

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.util.TextUtils;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

public class IdeaUtils {

    private static Long sVersionNum;

    private static String sVersionStr = null;

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

    public static void logEnvInfo() {
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

    public static PsiFile searchFileByClassName(Project project, String name, boolean withSuffix, String pkName) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        if (withSuffix) {
            PsiFile[] files = FilenameIndex.getFilesByName(project, name, new EverythingGlobalScope(project));
            if (isArrayEmpty(files)) {
                final int i = name.lastIndexOf(".");
                if (i > -1 && !name.endsWith("xml")) {
                    files = FilenameIndex.getFilesByName(project, name.substring(0, i) + ".class", new EverythingGlobalScope(project));
                }
                if (isArrayEmpty(files)) {
                    return null;
                }
            }
            return getSelfProjectFile(files, pkName);
        } else {
            PsiFile[] javaFiles = FilenameIndex.getFilesByName(project, name + ".java", new EverythingGlobalScope(project));
            PsiFile[] kotlinFiles = FilenameIndex.getFilesByName(project, name + ".kt", new EverythingGlobalScope(project));
            PsiFile[] sourceFiles = (PsiFile[]) ArrayUtils.addAll(javaFiles, kotlinFiles);
            if (isArrayEmpty(sourceFiles)) {
                if (isArrayEmpty(sourceFiles)) {
                    sourceFiles = FilenameIndex.getFilesByName(project, name + ".class", new EverythingGlobalScope(project));
                }
            } else {
                return getSelfProjectFile(sourceFiles, pkName);
            }
            if (isArrayEmpty(sourceFiles)) {
                return null;
            } else {
                return getSelfProjectFile(sourceFiles, pkName);
            }
        }
    }

    private static boolean isArrayEmpty(Object[] objects) {
        return objects == null || objects.length == 0;
    }

    public static boolean goSourceCodeLines(CodeLocatorWindow codeLocatorWindow, Project project, String name, int line, boolean withSuffix, String pkName) {
        PsiFile psiFile = searchFileByClassName(project, name, withSuffix, pkName);
        if (psiFile != null) {
            int finalLine = line;
            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, psiFile.getVirtualFile(), finalLine, 0);
            Editor editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
            doEditEffect(editor, finalLine);
            return true;
        }
        alert(codeLocatorWindow, project, name, pkName, "", line);
        return false;
    }

    /***
     * 无法确定具体行数，根据关键字搜索
     * @param project
     * @param name
     * @return
     */
    public static boolean goSourceCodeAndSearch(CodeLocatorWindow codeLocatorWindow, Project project, String name, String pre, String searchStr, boolean isMatch, boolean withSuffix, String pkName) {
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
            line = line > 0 ? line - 1 : Math.max(line, 0);
            editor.getScrollingModel().scrollTo(new LogicalPosition(line, 0), ScrollType.CENTER);
            doEditEffect(editor, line, realSearchStr);
            return true;
        }
        final GotoClassModel2 model = new GotoClassModel2(project);
        final Object[] elementsByName = model.getElementsByName(name, true, name);
        if (elementsByName.length > 0 && elementsByName[0] instanceof PsiElement) {
            if (NavigationUtil.openFileWithPsiElement((PsiElement) elementsByName[0], true, true)) {
                String finalSearchStr = searchStr;
                ThreadUtils.runOnUIThread(() -> {
                    final FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
                    if (selectedEditor instanceof PsiAwareTextEditorImpl) {
                        final Editor editor = ((PsiAwareTextEditorImpl) selectedEditor).getEditor();
                        Document document = editor.getDocument();
                        int line = findTextLineInFileContent(finalSearchStr, "", document.getText(), false);
                        line = line > 0 ? line - 1 : Math.max(line, 0);
                        editor.getScrollingModel().scrollTo(new LogicalPosition(line, 0), ScrollType.CENTER);
                        doEditEffect(editor, line, finalSearchStr);
                    }
                });
            }
            return true;
        }
        Log.e("找不到对应文件 name: " + name + ", searchStr: " + searchStr);
        alert(codeLocatorWindow, project, name, pkName, searchStr, -1);
        return false;
    }

    public static boolean goSourceCodeAndSearch(CodeLocatorWindow codeLocatorWindow, Project project, String name, String candidateName, String searchStr, String pkName) {
        PsiFile psiFile = null;
        PsiFile candidateFile = null;
        PsiFile[] files = null;
        final PsiClass aClass = JavaPsiFacade.getInstance(project).findClass(pkName + "." + name, new EverythingGlobalScope(project));
        if (aClass != null) {
            files = new PsiFile[1];
            files[0] = aClass.getContainingFile();
        }
        if (files == null) {
            PsiFile[] javaFiles = FilenameIndex.getFilesByName(project, name + ".java", new EverythingGlobalScope(project));
            PsiFile[] kotlinFiles = FilenameIndex.getFilesByName(project, name + ".kt", new EverythingGlobalScope(project));
            files = (PsiFile[]) ArrayUtils.addAll(javaFiles, kotlinFiles);
        }
        if (isArrayEmpty(files)) {
            if (candidateName != null && !candidateName.isEmpty()) {
                PsiFile[] candidateFiles = FilenameIndex.getFilesByName(project, candidateName + ".java", new EverythingGlobalScope(project));
                if (isArrayEmpty(candidateFiles)) {
                    candidateFiles = FilenameIndex.getFilesByName(project, candidateName + ".kt", new EverythingGlobalScope(project));
                }
                if (!isArrayEmpty(candidateFiles)) {
                    candidateFile = getSelfProjectFile(candidateFiles, pkName);
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
                    psiFile = getSelfProjectFile(files, pkName);
                }
            }
        } else {
            psiFile = getSelfProjectFile(files, pkName);
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
        final GotoClassModel2 model = new GotoClassModel2(project);
        final Object[] elementsByName = model.getElementsByName(name, true, name);
        if (elementsByName.length > 0 && elementsByName[0] instanceof PsiElement) {
            if (NavigationUtil.openFileWithPsiElement((PsiElement) elementsByName[0], true, true)) {
                ThreadUtils.runOnUIThread(() -> {
                    final FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
                    if (selectedEditor instanceof PsiAwareTextEditorImpl) {
                        final Editor editor = ((PsiAwareTextEditorImpl) selectedEditor).getEditor();
                        Document document = editor.getDocument();
                        int line = findTextLineInFileContent(searchStr, "", document.getText(), false);
                        line = line > 0 ? line - 1 : Math.max(line, 0);
                        editor.getScrollingModel().scrollTo(new LogicalPosition(line, 0), ScrollType.CENTER);
                        doEditEffect(editor, line, searchStr);
                    }
                });
            }
            return true;
        }
        Log.e("找不到对应文件 name: " + name + ", searchStr: " + searchStr);
        alert(codeLocatorWindow, project, name, pkName, searchStr, -1);
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
                    openFileLineAndSelectSearchStr(preEdit, includeXmlFile, includeFilesWithLine.get(includeXmlFile), project, realSearchStr, psiFile, line);
                    return true;
                }
                realSearchStr = "id=\"@android:id/" + searchId;
                line = findTextLineInFileContent(realSearchStr, "", layoutContent, false);
                Log.d("Search Include " + psiFile.getVirtualFile().getName() + ", key: " + realSearchStr + ", line: " + line);
                if (line > -1) {
                    openFileLineAndSelectSearchStr(preEdit, includeXmlFile, includeFilesWithLine.get(includeXmlFile), project, realSearchStr, psiFile, line);
                    return true;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private static void openFileLineAndSelectSearchStr(Editor preEdit, String preFileName, int lineInPreFile, Project project, String searchId, PsiFile psiFile, int line) {
        line = Math.max(0, line - 1);
        preEdit.getScrollingModel().scrollTo(new LogicalPosition(lineInPreFile, 0), ScrollType.CENTER);
        doEditEffect(preEdit, lineInPreFile, preFileName);
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, psiFile.getVirtualFile());
        Editor editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
        editor.getScrollingModel().scrollTo(new LogicalPosition(line, 0), ScrollType.CENTER);
        doEditEffect(editor, line, searchId);
    }

    public static void alert(CodeLocatorWindow codeLocatorWindow, Project project, String name, String pkName, String searchStr, int line) {
        new ShowSearchDialog(codeLocatorWindow, project, name, pkName, searchStr, line).showAndGet();
    }

    /***
     * 高亮标注
     */
    private static void doEditEffect(Editor editor, int lines) {
        if (lines <= 1) return;
        editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(lines, 0));
        editor.getSelectionModel().selectLineAtCaret();
    }

    /***
     * 高亮标注,某段文字
     */
    private static void doEditEffect(Editor editor, int lines, String searchText) {
        if (lines <= 1) return;
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
                    } else if (line.contains(pre)) {
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

    public static void navigateByJumpInfo(CodeLocatorWindow codeLocatorWindow, Project project, JumpInfo jumpInfo, boolean isMatch, String pre, String seachText, boolean withSuffix) {
        if (jumpInfo == null) {
            Log.e("jumpInfo == null");
            return;
        }
        String pkName = getPackageNameHasSuffix(jumpInfo.getFileName());
        try {
            if (!jumpInfo.needJumpById()) {
                int lineCount = Math.max(jumpInfo.getLineCount() - 1, 0);
                goSourceCodeLines(codeLocatorWindow, project, jumpInfo.getSimpleFileName(), lineCount, withSuffix, pkName);
            } else {
                goSourceCodeAndSearch(codeLocatorWindow, project, jumpInfo.getSimpleFileName(), pre, seachText, isMatch, withSuffix, pkName);
            }
        } catch (Exception e) {
            Log.e("跳转代码失败", e);
        }
    }

    public static String getRemovePackageFileName(String fullClassName) {
        if (TextUtils.isEmpty(fullClassName)) return "";
        final int indexOfPkgDot = fullClassName.lastIndexOf(".");
        if (indexOfPkgDot > -1) {
            return fullClassName.substring(indexOfPkgDot + 1);
        }
        return fullClassName;
    }

    public static void openBrowser(String webSite) throws URISyntaxException,
            IOException {
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

    public static PsiFile getSelfProjectFile(PsiFile[] files, String selfPackageName) {
        ArrayList<PsiFile> psiFiles = new ArrayList<>();
        for (PsiFile file : files) {
            Log.d("selfPackageName: " + selfPackageName + ", psiFilePathName: " + file.getViewProvider().getVirtualFile().getPresentableUrl());
            if (file.getViewProvider().getVirtualFile().getPresentableUrl().replace("/", ".").contains(selfPackageName)) {
                psiFiles.add(file);
            }
        }
        PsiFile result = psiFiles.size() > 0 ? psiFiles.get(0) : files[0];
        for (PsiFile psiFile : psiFiles) {
            if (psiFile.getViewProvider().getVirtualFile().toString().startsWith("file")
                    && !psiFile.getViewProvider().getVirtualFile().toString().contains(".gradle/caches")) {
                result = psiFile;
                break;
            }
        }
        if (result != null && result.getViewProvider().getVirtualFile().getPresentableUrl().endsWith("class")) {
            NotificationUtils.showNotification(result.getProject(), "Class文件匹配不精准, 请打开源码依赖或者检查aar是否上传源码", 3500L);
        }
        return result;
    }

    public static void startSync() {
        AnAction action = ActionManager.getInstance().getAction("Android.SyncProject");
        if (action == null) {
            return;
        }
        final Presentation presentation = action.getTemplatePresentation().clone();
        AnActionEvent event = new AnActionEvent(null, DataManager.getInstance().getDataContext(), "mock sync", presentation, ActionManager.getInstance(), 0);
        action.beforeActionPerformedUpdate(event);
        if (!presentation.isEnabled()) {
            return;
        }
        action.actionPerformed(event);
    }
}
