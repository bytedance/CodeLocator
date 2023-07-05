package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.action.AddSourceCodeAction;
import com.bytedance.tools.codelocator.model.*;
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow;
import com.bytedance.tools.codelocator.panels.ScreenPanel;
import com.google.gson.reflect.TypeToken;
import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

public class FileUtils {

    public static final String CHARSET_NAME = "UTF-8";

    public static final String LOG_FILE_NAME = "codeLocator_log.txt";

    public static final String UPLOAD_LOG_FILE_NAME = "grabInfo.codeLocator";

    public static final String CODE_LOCATOR_FILE_SUFFIX = ".codeLocator";

    public static final String CODE_LOCATOR_FILE_PREFIX = "CodeLocator_";

    public static final String CONFIG_FILE_NAME = "codeLocator_config.txt";

    public static final String SCHEMA_FILE_NAME = "codeLocator_schema_history.txt";

    public static final String UPDATE_TMP_FILE_NAME = "update.zip.tmp";

    public static final String UPDATE_FILE_NAME = "update.zip";

    public static final String VIEW_DATA_FILE_NAME = "codeLocator_view_data.json";

    public static final String SAVE_IMAGE_FILE_NAME = "screenCap.png";

    public static final String SAVE_QR_IMAGE_FILE_NAME = "qrCode.png";

    public static final String SAVE_GRAPH_FILE_NAME = "runTimeInfo.txt";

    public static final String GRAPH_COMMAND_FILE_NAME = "commandData.txt";

    public static final String DUMP_COMMAND_FILE_NAME = "dump.txt";

    public static final String GRAPH_COLOR_DATA_FILE_NAME = "colorData.txt";

    public static final String EXEC_LIST_FILE_NAME = "exec.txt";

    public static final String TEMP_UIX_FILE_NAME = "codeLocator_temp.uix";

    public static final String CONFIG_INFO_FILE_NAME = "codeLocator_project_config.txt";

    public static final String ANDROID_MODULE_TEMPLATE_FILE_NAME = "AndroidModuleTemplate.zip";

    public static final String JAVA_MODULE_TEMPLATE_FILE_NAME = "JarModuleTemplate.zip";

    public static final SimpleDateFormat FILE_NAME_FORMAT = new SimpleDateFormat("MMdd_HHmmss");

    public static String sUserDesktopPath;

    public static String sCodeLocatorMainDirPath;

    public static String sCodelocatorTmpFileDirPath;

    public static String sCodelocatorHistoryFileDirPath;

    public static String sCodelocatorImageFileDirPath;

    public static String sPluginInstallDir = null;

    public static String sCodeLocatorPluginDir = null;

    public static String sLogFilePath;

    public static void init() {
        if (sLogFilePath != null) {
            return;
        }
        initCodeLocatorDir();
        initLogFile();
        initPluginInstallPath();
        OSHelper.getInstance().init();
    }

    public static void saveScreenCap(Image image) {
        saveImageToFile(image, new File(sCodeLocatorMainDirPath, SAVE_IMAGE_FILE_NAME));
    }

    public static boolean canOpenFile(File file) {
        if (file == null || !file.exists() || file.getName() == null || file.isDirectory()) {
            return false;
        }
        final String name = file.getName().toLowerCase();
        String[] supportFileSuffix = new String[]{
            ".png", ".jpg", ".jpeg", ".txt", ".json", ".css", ".js", ".html"
        };
        for (String suffix : supportFileSuffix) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    public static void saveImageToFile(Image image, File file) {
        try {
            if (image == null) {
                return;
            }
            if (image instanceof BufferedImage) {
                ImageIO.write((BufferedImage) image, "PNG", new FileOutputStream(file));
            } else {
                BufferedImage bufferedImage = new BufferedImage(image.getWidth(null),
                    image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                bufferedImage.getGraphics().drawImage(image, 0, 0, null);
                ImageIO.write(bufferedImage, "PNG", new FileOutputStream(file));
            }
        } catch (Exception e) {
            Log.e("保存图片失败", e);
        }
    }

    public static void saveRuntimeInfo(String info) {
        saveContentToFile(new File(sCodeLocatorMainDirPath, SAVE_GRAPH_FILE_NAME).getAbsolutePath(), Log.getTimeStamp() + "\n" + info);
    }

    public static void saveCommandData(String info) {
        saveContentToFile(new File(sCodeLocatorMainDirPath, GRAPH_COMMAND_FILE_NAME).getAbsolutePath(), Log.getTimeStamp() + "\n" + info);
    }

    public static void saveDumpData(String dumpInfo) {
        saveContentToFile(new File(sCodeLocatorMainDirPath, DUMP_COMMAND_FILE_NAME).getAbsolutePath(), Log.getTimeStamp() + "\n" + dumpInfo);
    }

    public static WFile getCodeLocatorFile(WFile rootFile, int androidVersion) {
        if (androidVersion >= CodeLocatorConstants.USE_TRANS_FILE_SDK_VERSION) {
            final WFile wFile = new WFile();
            wFile.setAbsoluteFilePath(CodeLocatorConstants.BASE_DIR_PATH);
            wFile.setName(CodeLocatorConstants.BASE_DIR_NAME);
            wFile.setDirectory(true);
            return wFile;
        }
        LinkedList<WFile> linkedList = new LinkedList();
        linkedList.add(rootFile);
        while (!linkedList.isEmpty()) {
            int size = linkedList.size();
            for (int i = 0; i < size; i++) {
                WFile file = linkedList.remove(0);
                if (file.isDirectory() && file.isInSDCard() && CodeLocatorConstants.BASE_DIR_NAME.equals(file.getName())) {
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

    private static ProjectConfig sProjectConfig;

    public static ProjectConfig getConfig() {
        if (sProjectConfig != null) {
            return sProjectConfig;
        }
        final File file = new File(sCodeLocatorMainDirPath, CONFIG_INFO_FILE_NAME);
        if (!file.exists()) {
            return new ProjectConfig();
        }
        try {
            final String fileContent = FileUtils.getFileContent(file);
            sProjectConfig = GsonUtils.sGson.fromJson(fileContent, ProjectConfig.class);
            if (sProjectConfig == null) {
                sProjectConfig = new ProjectConfig();
            }
            return sProjectConfig;
        } catch (Throwable t) {
            return new ProjectConfig();
        }
    }

    public static void saveConfig(ProjectConfig projectConfig) {
        sProjectConfig = projectConfig;
        final File file = new File(sCodeLocatorMainDirPath, CONFIG_INFO_FILE_NAME);
        if (projectConfig == null) {
            if (file.exists()) {
                file.delete();
                return;
            }
        }
        FileUtils.saveContentToFile(file, GsonUtils.sGson.toJson(projectConfig));
    }

    public static List<VirtualFile> getMatchFileList(VirtualFile[] files, Predicate<VirtualFile> predicate, boolean breakWhenFoundOne) {
        List<VirtualFile> result = new LinkedList<>();
        if (files == null) {
            return result;
        }
        for (VirtualFile file : files) {
            if (file.isDirectory()) {
                if ("build".equals(file.getName())) {
                    continue;
                }
                result.addAll(getMatchFileList(file.getChildren(), predicate, breakWhenFoundOne));
                if (breakWhenFoundOne && !result.isEmpty()) {
                    break;
                }
            } else {
                if (predicate.test(file)) {
                    result.add(file);
                    if (breakWhenFoundOne) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public static ExecInfo getVersionExecInfo(String version) {
        final File file = new File(sCodeLocatorMainDirPath, EXEC_LIST_FILE_NAME);
        if (!file.exists()) {
            return null;
        }
        try {
            final String fileContent = FileUtils.getFileContent(file);
            final List<ExecInfo> execInfos = GsonUtils.sGson.fromJson(fileContent, new TypeToken<List<ExecInfo>>() {
            }.getType());
            if (execInfos == null || execInfos.isEmpty()) {
                return null;
            }
            for (ExecInfo info : execInfos) {
                if (info.version.equals(version)) {
                    return info;
                }
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void saveExec(ExecInfo execInfo) {
        if (execInfo == null) {
            return;
        }
        final File file = new File(sCodeLocatorMainDirPath, EXEC_LIST_FILE_NAME);
        List<ExecInfo> execInfos = null;
        if (file.exists()) {
            final String fileContent = FileUtils.getFileContent(file);
            execInfos = GsonUtils.sGson.fromJson(fileContent, new TypeToken<List<ExecInfo>>() {}.getType());
        }
        if (execInfos == null) {
            execInfos = new LinkedList<>();
        }
        execInfos.remove(execInfo);
        execInfos.add(execInfo);
        FileUtils.saveContentToFile(file, GsonUtils.sGson.toJson(execInfos));
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

    private static void initPluginInstallPath() {
        File pluginPath = PluginManager.getPlugin(PluginId.getId("com.bytedance.tools.codelocator")).getPath();
        if (pluginPath.exists()) {
            sPluginInstallDir = pluginPath.getParent();
            sCodeLocatorPluginDir = pluginPath.getAbsolutePath();
        }
    }

    private static void initCodeLocatorDir() {
        final String userHomePath = System.getProperty("user.home");
        sUserDesktopPath = OSHelper.getInstance().getUserDesktopFilePath();
        initMainFileDir(userHomePath);
        initTmpFileDir();
        initHistoryFileDir();
        initImageFileDir();
        deleteChildFile(sCodelocatorImageFileDirPath);
        deleteChildFile(sCodelocatorTmpFileDirPath);
    }

    private static void initMainFileDir(String userHomePath) {
        final File file = new File(userHomePath, ".codeLocator_main");
        sCodeLocatorMainDirPath = file.getPath();
        if (!file.exists()) {
            final boolean mkdirs = file.mkdirs();
            if (!mkdirs && sUserDesktopPath.equals(userHomePath)) {
                initMainFileDir(sUserDesktopPath);
            }
        } else if (!file.isDirectory()) {
            file.delete();
            file.mkdirs();
        }
    }

    private static void initImageFileDir() {
        final File file = new File(sCodeLocatorMainDirPath, "image");
        sCodelocatorImageFileDirPath = file.getPath();
        if (!file.exists()) {
            file.mkdirs();
        } else if (!file.isDirectory()) {
            file.delete();
            file.mkdirs();
        }
    }

    private static void initHistoryFileDir() {
        final File file = new File(sCodeLocatorMainDirPath, "historyFile");
        sCodelocatorHistoryFileDirPath = file.getPath();
        if (!file.exists()) {
            file.mkdirs();
        } else if (!file.isDirectory()) {
            file.delete();
            file.mkdirs();
        }
    }

    private static void initTmpFileDir() {
        final File file = new File(sCodeLocatorMainDirPath, "tempFile");
        sCodelocatorTmpFileDirPath = file.getPath();
        if (!file.exists()) {
            file.mkdirs();
        } else if (!file.isDirectory()) {
            file.delete();
            file.mkdirs();
        }
    }

    private static void initLogFile() {
        File logFile = new File(sCodeLocatorMainDirPath, LOG_FILE_NAME);
        sLogFilePath = logFile.getAbsolutePath();
        if (!logFile.exists()) {
            createLogFile();
        } else {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sLogFilePath), FileUtils.CHARSET_NAME));
                final String timeStart = reader.readLine().trim();
                reader.close();
                final Long lastUpdateTime = Long.valueOf(timeStart);
                if (System.currentTimeMillis() - lastUpdateTime > 3 * 60L * 60 * 24 * 1000) {
                    createLogFile();
                    FileUtils.deleteChildFile(FileUtils.sCodelocatorTmpFileDirPath);
                }
            } catch (Exception e) {
                createLogFile();
            }
        }
        IdeaUtils.logVersion();
    }

    private static void createLogFile() {
        final File file = new File(sLogFilePath);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(sLogFilePath));
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
        final String projectPath = FileUtils.getProjectFilePath(project);
        if (projectPath == null || projectPath.isEmpty()) {
            return false;
        }
        File settingsFile = new File(projectPath, "settings.gradle");
        if (!settingsFile.exists() || !settingsFile.isFile()) {
            return false;
        }
        String fileContent = getFileContent(settingsFile);
        return fileContent.contains("if (new File(\"" + AddSourceCodeAction.CODE_LOCATOR_GRADLE_FILE_NAME + "\").exists()) {") && new File(projectPath, AddSourceCodeAction.CODE_LOCATOR_GRADLE_FILE_NAME).exists();
    }

    public static void deleteChildFile(String filePath) {
        deleteChildFile(new File(filePath));
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
        if (level >= 3 || !file.isDirectory() || file.getName().startsWith(".")) {
            return;
        }
        final File buildPath = new File(file, "build" + File.separator + "outputs" + File.separator + "apk");
        if (buildPath.exists()) {
            sets.add(buildPath.getAbsolutePath());
            return;
        }
        final File[] files = file.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                addHasApkFilePath(sets, f, level + 1);
            } else if (f.getName().endsWith(".apk")) {
                sets.add(f.getParent());
            }
        }
    }

    public static HashSet<String> getMainModuleName(Project project, boolean findAllModule) {
        HashSet<String> results = new HashSet<>();
        final Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            try {
                final AndroidFacet instance = AndroidFacet.getInstance(module);
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
                    if (module.getName().contains(".")) {
                        results.add(module.getName().substring(module.getName().lastIndexOf(".") + 1));
                    } else {
                        results.add(module.getName());
                    }
                    if (!findAllModule) {
                        break;
                    }
                }
            } catch (Throwable t) {
                Log.e("getMainModuleName error", t);
            }
        }
        if (results.isEmpty()) {
            return getMainModuleName(project.getProjectFilePath(), findAllModule);
        }
        return results;
    }

    public static HashSet<String> getMainModuleName(String projectPath, boolean findAllModule) {
        HashSet<String> results = new HashSet<>();
        results.add("app");
        File workSpaceXmlFile = new File(projectPath, ".idea" + File.separator + "workspace.xml");
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

    public static File findFileByName(File root, String findFileName, int maxLevel, Comparator<File> fileComparator) {
        return findFileByName(root, findFileName, maxLevel, 0, fileComparator);
    }

    private static File findFileByName(File rootFile, String findFileName, int maxLevel, int currentLevel, Comparator<File> fileComparator) {
        if (findFileName == null) {
            return null;
        }
        if (findFileName.equals(rootFile.getName())) {
            return rootFile;
        }
        if (rootFile.isDirectory()) {
            final File[] files = rootFile.listFiles();
            if (files == null) {
                return null;
            }
            final List<File> fileList = Arrays.asList(files);
            if (fileComparator != null) {
                fileList.sort(fileComparator);
            }
            for (File f : fileList) {
                if (maxLevel < 0 || currentLevel < maxLevel) {
                    File file = findFileByName(f, findFileName, maxLevel, currentLevel + 1, fileComparator);
                    if (file != null) {
                        return file;
                    }
                }
            }
        }
        return null;
    }

    public static String getCurrentBranchName(Project project) {
        if (project != null && project.getProjectFile() != null) {
            final Repository repository = VcsRepositoryManager.getInstance(project).getRepositoryForFile(project.getProjectFile());
            if (repository != null) {
                return repository.getCurrentBranchName();
            }
        }
        return null;
    }

    private static HashMap<String, String> sProjectGitUrlMap = new HashMap<>();

    public static String getProjectGitUrl(String projectPath) {
        if (projectPath == null || projectPath.isEmpty()) {
            return projectPath;
        }
        final String gitUrl = sProjectGitUrlMap.get(projectPath);
        if (gitUrl != null && !gitUrl.isEmpty()) {
            return gitUrl;
        }
        if (!new File(projectPath, ".git").exists()) {
            return projectPath;
        }
        final String gitUrlByPath = OSHelper.getInstance().getGitUrlByPath(projectPath);
        if (!gitUrlByPath.isEmpty()) {
            sProjectGitUrlMap.put(projectPath, gitUrlByPath);
        }
        return gitUrlByPath;
    }

    public static String getProjectGitUrl(Project project) {
        return getProjectGitUrl(FileUtils.getProjectFilePath(project));
    }

    public static String getProjectGitUrl(Repository repository) {
        if (repository == null) {
            return "";
        }
        final String repoInfoStr = repository.toLogString();
        final int myPushUrlsIndex = repoInfoStr.indexOf("myPushUrls=[");
        if (myPushUrlsIndex > -1) {
            final int myPushEnd = repoInfoStr.indexOf("]", myPushUrlsIndex + "myPushUrls=[".length());
            if (myPushEnd > -1) {
                final String substring = repoInfoStr.substring(myPushUrlsIndex + "myPushUrls=[".length(), myPushEnd);
                if (!substring.contains(",")) {
                    return substring.trim();
                } else {
                    final String[] split = substring.split(",");
                    return split[0].trim();
                }
            }
        }
        return "";
    }

    @NotNull
    public static ByteArrayOutputStream readByteArrayOutputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream;
        try {
            byte[] buffer = new byte[4096];
            int readLength = -1;
            byteArrayOutputStream = new ByteArrayOutputStream();
            while ((readLength = inputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, readLength);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                //关闭流
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return byteArrayOutputStream;
    }

    private static HashMap<String, String> sProjectPathMap = new HashMap<>();

    public static String getProjectFilePath(Project project) {
        String path = sProjectPathMap.get(project.getBasePath());
        if (path != null) {
            return path;
        }
        if (project.getBasePath() == null) {
            return "";
        }
        path = new File(project.getBasePath()).getAbsolutePath();
        sProjectPathMap.put(project.getBasePath(), path);
        return path;
    }

    public static void saveGrabInfo(CodeLocatorWindow codeLocatorWindow) {
        try {
            if (codeLocatorWindow != null && codeLocatorWindow.getScreenPanel() != null) {
                final ScreenPanel screenPanel = codeLocatorWindow.getScreenPanel();
                final Image screenCapImage = screenPanel.getScreenCapImage();
                final WApplication application = screenPanel.getApplication();
                if (screenCapImage != null && application != null) {
                    final CodeLocatorInfo codelocatorInfo = new CodeLocatorInfo(application, screenCapImage);
                    FileUtils.saveContentToFile(new File(FileUtils.sCodeLocatorMainDirPath, FileUtils.UPLOAD_LOG_FILE_NAME), codelocatorInfo.toBytes());
                }
            }
        } catch (Throwable t) {
            Log.e("保存抓取信息失败", t);
        }
    }

}