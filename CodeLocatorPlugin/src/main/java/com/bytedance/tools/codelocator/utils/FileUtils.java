package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.model.ExecResult;
import com.bytedance.tools.codelocator.model.WFile;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedList;

public class FileUtils {

    public static final String CHARSET_NAME = "UTF-8";

    public static final String LOG_FILE_NAME = "codelocator_log.txt";

    public static final String UPLOAD_LOG_FILE_NAME = "grabInfo.codelocator";

    public static final String CONFIG_FILE_NAME = "codelocator_config.txt";

    public static final String SCHEMA_FILE_NAME = "codelocator_schema_history.txt";

    public static final String UPDATE_TMP_FILE_NAME = "update.zip.tmp";

    public static final String UPDATE_FILE_NAME = "update.zip";

    public static final String SAVE_IMAGE_FILE_NAME = "screenCap.png";

    public static final String SAVE_GRAPH_FILE_NAME = "runTimeInfo.txt";

    public static final String GRAPH_COMMAND_FILE_NAME = "commandData.txt";

    public static final String ANDROID_MODULE_TEMPLATE_FILE_NAME = "AndroidModuleTemplate.zip";

    public static final String JAVA_MODULE_TEMPLATE_FILE_NAME = "JarModuleTemplate.zip";

    public static final String UNLOCK_PHONE_IF_NEED_FILE = "unlockPhoneIfNeed";

    public static final SimpleDateFormat FILE_NAME_FORMAT = new SimpleDateFormat("MMdd_HHmmss");

    public static File codelocatorMainDir;

    public static File codelocatorTmpFileDir;

    public static File codelocatorHistoryFileDir;

    public static String sProjectName = "unknown";

    public static String sPkgName = "unknown";

    public static File pluginInstallDir = null;

    public static File codelocatorPluginDir = null;

    public static String pluginInstallDirShellPath = null;

    public static File logFile;

    public static String ADB_PATH;

    public static void init(Project project) {
        if (project == null) {
            return;
        }
        if (sProjectName == null || (project.getBasePath() != null && !project.getBasePath().contains(sProjectName))) {
            if (project.getBasePath() != null) {
                final File file = new File(project.getBasePath());
                if (file.exists()) {
                    sProjectName = file.getName();
                }
            }
        }
        if (logFile != null) {
            return;
        }
        initCodeLocatorDir();
        initLogFile();
        initPluginInstallPath(project);
        initProjectGitPath(project);
    }

    public static void initProjectGitPath(Project project) {
        if (project == null) {
            return;
        }
        try {
            final String basePath = project.getBasePath();
            final ExecResult execResult = ShellHelper.execCommand("cd " + basePath.replace(" ", "\\ ") + "; git remote -v");
            if (execResult.getResultCode() == 0) {
                Mob.mob(Mob.Action.EXEC, "remote: " + new String(execResult.getResultBytes()).replace("\n", ";").replace("origin\t", ""));
            }
        } catch (Exception e) {
            Log.e("获取路径失败", e);
        }
    }

    public static void saveScreenCap(byte[] bytes) {
        try {
            if (bytes == null || bytes.length <= 0) {
                return;
            }
            OutputStream outputStream = new FileOutputStream(new File(codelocatorMainDir, SAVE_IMAGE_FILE_NAME));
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            Log.e("保存图片失败", e);
        }
    }

    public static void saveScreenCap(Image image) {
        try {
            if (image == null) {
                return;
            }
            if (image instanceof BufferedImage) {
                ImageIO.write((BufferedImage) image, "PNG", new FileOutputStream(new File(codelocatorMainDir, SAVE_IMAGE_FILE_NAME)));
            } else {
                BufferedImage bufferedImage = new BufferedImage(image.getWidth(null),
                        image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                bufferedImage.getGraphics().drawImage(image, 0, 0, null);
                ImageIO.write(bufferedImage, "PNG", new FileOutputStream(new File(codelocatorMainDir, SAVE_IMAGE_FILE_NAME)));
            }
        } catch (Exception e) {
            Log.e("保存图片失败", e);
        }
    }

    public static void saveScreenInfo(String info) {
        saveContentToFile(new File(codelocatorMainDir, SAVE_GRAPH_FILE_NAME).getAbsolutePath(), Log.getTimeStamp() + "\n" + info);
    }

    public static void saveCommandData(String info) {
        saveContentToFile(new File(codelocatorMainDir, GRAPH_COMMAND_FILE_NAME).getAbsolutePath(), Log.getTimeStamp() + "\n" + info);
    }

    public static WFile getAndroidDownloadFile() {
        WFile file = new WFile();
        file.setAbsoluteFilePath("/sdcard/Download");
        file.setInSDCard(true);
        file.setName("Download");
        file.setDirectory(true);
        return file;
    }

    public static WFile getCodeLocatorFile(WFile rootFile) {
        LinkedList<WFile> linkedList = new LinkedList();
        linkedList.add(rootFile);
        while (!linkedList.isEmpty()) {
            int size = linkedList.size();
            for (int i = 0; i < size; i++) {
                WFile file = linkedList.remove(0);
                if (file.isDirectory() && file.isInSDCard() && "codelocator".equals(file.getName())) {
                    return file;
                }
                for (int index = 0; index < file.getChildCount(); index++) {
                    linkedList.add(file.getChildAt(index));
                }
            }
        }
        return null;
    }

    public static void saveContentToFile(String filePath, String info) {
        try {
            saveContentToFile(filePath, info.getBytes(CHARSET_NAME));
        } catch (UnsupportedEncodingException e) {
            Log.e("保存文件失败", e);
        }
    }

    public static boolean saveContentToFile(File file, String info) {
        try {
            return saveContentToFile(file, info.getBytes(CHARSET_NAME));
        } catch (UnsupportedEncodingException e) {
            Log.e("保存文件失败", e);
            return false;
        }
    }

    public static boolean saveContentToFile(File file, byte[] contents) {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStream outputStream = new FileOutputStream(file);
            outputStream.write(contents);
            outputStream.flush();
            outputStream.close();
            return true;
        } catch (Exception e) {
            Log.e("保存文件失败", e);
            return false;
        }
    }

    public static void saveContentToFile(String filePath, byte[] contents) {
        saveContentToFile(new File(filePath), contents);
    }

    private static void findPluginInstallDir() {
        final String pluginsPath = PathManager.getPluginsPath();
        final File file = new File(pluginsPath);
        if (file.exists() && file.isDirectory()) {
            File pluginAdb = new File(file, "CodeLocatorPlugin/adb");
            if (pluginAdb.exists()) {
                pluginInstallDir = file;
                pluginInstallDirShellPath = pluginInstallDir.getAbsolutePath().replace(" ", "\\ ");
                ADB_PATH = pluginAdb.getParentFile().getAbsolutePath().replace(" ", "\\ ");
                codelocatorPluginDir = pluginAdb.getParentFile();
            }
        }
    }

    private static void initPluginInstallPath(Project project) {
        findPluginInstallDir();
        if (pluginInstallDir != null) {
            Log.d("pluginInstallDir " + pluginInstallDir + ", adbPath: " + ADB_PATH);
            initAdb();
            return;
        }
        Log.e("插件路径异常, pluginInstallDir " + pluginInstallDir + ", adbPath: " + ADB_PATH);
        Messages.showMessageDialog(project, "插件路径异常 请点击反馈图标进行反馈", "CodeLocator", Messages.getInformationIcon());
    }

    public static void initAdb() {
        try {
            checkNeedUseSystemAdb();
            ShellHelper.execCommand("chmod a+x " + ADB_PATH + "/adb");
            ShellHelper.execCommand("chmod a+x " + ADB_PATH + "/restartAndroidStudio");
            ShellHelper.execCommand("chmod a+x " + ADB_PATH + "/" + UNLOCK_PHONE_IF_NEED_FILE);
            Log.d("ADB_PATH " + ADB_PATH);
        } catch (Exception e) {
            Log.e("修改Adb权限失败 ADB_PATH: " + ADB_PATH, e);
        }
    }

    private static void checkNeedUseSystemAdb() {
        final File pluginAdb = new File(pluginInstallDir, "CodeLocatorPlugin/adb");
        File tmpAdbFile = null;
        try {
            final ExecResult result = ShellHelper.execCommand("which adb");
            if (result.getResultCode() == 0) {
                String systemAdbPath = new String(result.getResultBytes(), FileUtils.CHARSET_NAME).trim();
                Log.d("systemAdbPath: " + systemAdbPath);
                if (!systemAdbPath.isEmpty()) {
                    if (systemAdbPath.contains("\n")) {
                        final String[] splitLines = systemAdbPath.split("\n");
                        if (splitLines.length > 0) {
                            systemAdbPath = splitLines[0];
                        }
                    }
                    final File systemAdb = new File(systemAdbPath);
                    boolean needReplace = systemAdb.exists() && pluginAdb.length() != systemAdb.length();
                    Log.d("插件ADB: " + pluginAdb + ", 大小: " + pluginAdb.length() + ", 系统ADB: " + systemAdb + ", 大小: " + systemAdb.length() + ", 是否替换: " + needReplace);
                    if (!needReplace) {
                        return;
                    }
                    tmpAdbFile = new File(pluginAdb.getParent(), "adb.tmp");
                    pluginAdb.renameTo(tmpAdbFile);
                    final Path copy = Files.copy(systemAdb.toPath(), pluginAdb.toPath());
                    if (copy != null && pluginAdb.exists()) {
                        tmpAdbFile.delete();
                    }
                }
            } else {
                if (result.getErrorBytes() != null) {
                    final String s = new String(result.getErrorBytes());
                    if (!s.trim().isEmpty()) {
                        Log.e("Check adb result " + s);
                    }
                }
            }
        } catch (Throwable t) {
            Log.e("检测系统Adb失败 " + ADB_PATH, t);
            if (tmpAdbFile != null && tmpAdbFile.exists()) {
                if (pluginAdb.exists() && pluginAdb.length() > 0) {
                    tmpAdbFile.delete();
                } else {
                    if (pluginAdb.exists()) {
                        pluginAdb.delete();
                    }
                    tmpAdbFile.renameTo(pluginAdb);
                }
            }
        }
    }

    private static void initCodeLocatorDir() {
        final String userHomePath = System.getProperty("user.home");
        codelocatorMainDir = new File(userHomePath, ".codelocator_main");
        if (!codelocatorMainDir.exists()) {
            codelocatorMainDir.mkdirs();
        } else if (!codelocatorMainDir.isDirectory()) {
            codelocatorMainDir.delete();
            codelocatorMainDir.mkdirs();
        }
        codelocatorTmpFileDir = new File(codelocatorMainDir, "tempFile");
        if (!codelocatorTmpFileDir.exists()) {
            codelocatorTmpFileDir.mkdirs();
        } else if (!codelocatorTmpFileDir.isDirectory()) {
            codelocatorTmpFileDir.delete();
            codelocatorTmpFileDir.mkdirs();
        }
        codelocatorHistoryFileDir = new File(codelocatorMainDir, "historyFile");
        if (!codelocatorHistoryFileDir.exists()) {
            codelocatorHistoryFileDir.mkdirs();
        } else if (!codelocatorHistoryFileDir.isDirectory()) {
            codelocatorHistoryFileDir.delete();
            codelocatorHistoryFileDir.mkdirs();
        }
    }

    private static void initLogFile() {
        logFile = new File(codelocatorMainDir, LOG_FILE_NAME);
        if (!logFile.exists()) {
            createLogFile();
        } else {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(logFile));
                final String timeStart = reader.readLine().trim();
                reader.close();
                final Long lastUpdateTime = Long.valueOf(timeStart);
                if (System.currentTimeMillis() - lastUpdateTime > 3 * 60L * 60 * 24 * 1000) {
                    createLogFile();
                    FileUtils.deleteChildFile(FileUtils.codelocatorTmpFileDir);
                }
            } catch (Exception e) {
                createLogFile();
            }
        }
        IdeaUtils.logEnvInfo();
    }

    private static void createLogFile() {
        if (logFile.exists()) {
            logFile.delete();
        }
        try {
            logFile.createNewFile();
            final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logFile));
            bufferedWriter.write(System.currentTimeMillis() + "\n");
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFileContent(String filePath) {
        return getFileContent(new File(filePath));
    }

    public static String getFileContent(File file) {
        try {
            final byte[] fileContentBytes = getFileContentBytes(file);
            if (fileContentBytes != null) {
                return new String(fileContentBytes, CHARSET_NAME);
            }
        } catch (Exception e) {
            Log.e("读取文件失败 " + file.getAbsolutePath(), e);
        }
        return "";
    }

    public static byte[] getFileContentBytes(File file) {
        if (!file.exists() || file.isDirectory()) {
            return null;
        }
        byte[] fileBytes = new byte[(int) file.length()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(fileBytes);
            in.close();
            return fileBytes;
        } catch (Exception e) {
            Log.e("读取文件失败 " + file.getAbsolutePath(), e);
        }
        return null;
    }

    public static String getFileContent(VirtualFile file) {
        if (!file.exists() || file.isDirectory()) {
            return "";
        }
        byte[] fileContent = new byte[(int) file.getLength()];
        try {
            InputStream in = file.getInputStream();
            in.read(fileContent);
            in.close();
            return new String(fileContent, "UTF-8");
        } catch (Exception e) {
            Log.e("读取文件失败 " + file.getCanonicalPath(), e);
        }
        return "";
    }

    public static boolean isHasCodeIndexModule(Project project) {
        final String projectPath = project.getBasePath();
        if (projectPath == null || projectPath.isEmpty()) {
            return false;
        }
        File settingsFile = new File(projectPath, "settings.gradle");
        if (!settingsFile.exists() || !settingsFile.isFile()) {
            return false;
        }
        String fileContent = getFileContent(settingsFile);
        if (fileContent == null) {
            return false;
        }
        return fileContent.contains("if (new File(\"codelocator.gradle\").exists()) {") && new File(projectPath, "codelocator.gradle").exists();
    }

    public static void deleteChildFile(File file) {
        if (file == null || !file.exists() || !file.isDirectory()) {
            return;
        }
        final File[] files = file.listFiles();
        for (File f : files) {
            deleteFile(f);
        }
    }

    public static void deleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            for (File f : files) {
                deleteFile(f);
            }
            file.delete();
        } else {
            file.delete();
        }
    }

    public static HashSet<String> getHasApkFilePath(String projectPath) {
        HashSet sets = new HashSet();
        addHasApkFilePath(sets, new File(projectPath), 0);
        return sets;
    }

    private static void addHasApkFilePath(HashSet<String> sets, File file, int level) {
        if (level >= 3 || !file.isDirectory()) {
            return;
        }
        final File buildPath = new File(file, "/build/outputs/apk");
        if (buildPath.exists()) {
            sets.add(buildPath.getAbsolutePath());
            return;
        }
        final File[] files = file.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                addHasApkFilePath(sets, f, level + 1);
            }
        }
    }

    public static HashSet<String> getMainModuleName(String projectPath, boolean findAllModule) {
        HashSet<String> results = new HashSet<>();
        results.add("app");
        File workSpaceXmlFile = new File(projectPath, ".idea/workspace.xml");
        if (!workSpaceXmlFile.exists()) {
            return results;
        }
        String fileContent = FileUtils.getFileContent(workSpaceXmlFile);
        if (fileContent == null || fileContent.length() == 0) {
            return results;
        }
        String[] splitLines = fileContent.split("\n");
        results.clear();
        String selectAppStartStr = "selected=\"Android App.";
        String startOfMainModule = "name=\"";
        for (String line : splitLines) {
            if (line.contains(selectAppStartStr)) {
                int indexOfStart = line.indexOf(selectAppStartStr);
                int indexOfEnd = line.indexOf("\"", indexOfStart + selectAppStartStr.length());
                if (indexOfEnd > 0) {
                    String appModuleName = line.substring(indexOfStart + selectAppStartStr.length(), indexOfEnd).trim();
                    if (!findAllModule) {
                        results.clear();
                        results.add(appModuleName);
                        return results;
                    } else {
                        results.add(appModuleName);
                    }
                }
            } else if (line.contains("type=\"AndroidRunConfigurationType\"")) {
                int indexOfStart = line.indexOf(startOfMainModule);
                if (indexOfStart > -1) {
                    int indexOfEnd = line.indexOf("\"", indexOfStart + startOfMainModule.length());
                    if (indexOfEnd > -1) {
                        String appModuleName = line.substring(indexOfStart + startOfMainModule.length(), indexOfEnd).trim();
                        results.add(appModuleName);
                    }
                }
            }
        }
        if (results.isEmpty()) {
            results.add("app");
        }
        return results;
    }

    public static void copyFile(String sourceFilePath, String targetFilePath) {
        copyFile(new File(sourceFilePath), new File(targetFilePath));
    }

    public static void copyFile(File sourceFile, File targetFile) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            if (!targetFile.exists()) {
                targetFile.createNewFile();
            }
            inputStream = new FileInputStream(sourceFile);
            outputStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            System.out.println("Copy Error " + e);
        }
    }
}
