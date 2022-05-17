package com.bytedance.tools.codelocator.utils;

import com.android.ddmlib.Client;
import com.android.layoutinspector.LayoutInspectorCaptureOptions;
import com.android.layoutinspector.ProtocolVersion;
import com.android.layoutinspector.model.ClientWindow;
import com.android.layoutinspector.model.ViewNode;
import com.android.layoutinspector.parser.ViewNodeV2Parser;
import com.bytedance.tools.codelocator.device.DeviceManager;
import com.bytedance.tools.codelocator.device.action.AdbAction;
import com.bytedance.tools.codelocator.device.action.AdbCommand;
import com.bytedance.tools.codelocator.listener.CodeLocatorApplicationInitializedListener;
import com.bytedance.tools.codelocator.parser.CodeLocatorViewNodeParser;
import com.bytedance.tools.codelocator.parser.DumpInfoParser;
import com.bytedance.tools.codelocator.parser.JumpParser;
import com.bytedance.tools.codelocator.model.*;
import com.bytedance.tools.codelocator.response.StringResponse;
import com.intellij.openapi.project.Project;
import kotlin.Pair;
import kotlin.Triple;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DataUtils {

    private static volatile String sCurrentProjectName = "unknown";

    private static volatile String sCurrentApkName = "unknown";

    private static volatile String sCurrentSDKVersion = "unknown";

    private static volatile String sUserName = null;

    private static volatile String sUid = null;

    public static String getUserName() {
        if (sUserName != null) {
            return sUserName;
        }
        synchronized (DataUtils.class) {
            if (sUserName == null) {
                sUserName = OSHelper.getInstance().getUserName();
            }
        }

        return sUserName;
    }

    public static String getUid() {
        if (sUid != null) {
            return sUid;
        }
        synchronized (DataUtils.class) {
            if (sUid == null) {
                sUid = OSHelper.getInstance().getUid();
            }
        }
        return sUid;
    }

    public static String getCurrentProjectName() {
        return sCurrentProjectName;
    }

    public static void setCurrentProjectName(String currentProjectName) {
        if (currentProjectName == null || currentProjectName.isEmpty()) {
            currentProjectName = "unknown";
        }
        DataUtils.sCurrentProjectName = currentProjectName;
    }

    public static String getCurrentApkName() {
        return sCurrentApkName;
    }

    public static void setCurrentApkName(String currentApkName) {
        if (currentApkName == null || currentApkName.isEmpty()) {
            currentApkName = "unknown";
        }
        DataUtils.sCurrentApkName = currentApkName;
    }

    public static String getCurrentSDKVersion() {
        return sCurrentSDKVersion;
    }

    public static void setCurrentSDKVersion(String currentSDKVersion) {
        if (currentSDKVersion == null || currentSDKVersion.isEmpty()) {
            currentSDKVersion = "unknown";
        }
        DataUtils.sCurrentSDKVersion = currentSDKVersion;
    }

    public static void restoreAllStructInfo(WApplication application, boolean fromFile) {
        if (!fromFile) {
            application.setGrabTime(System.currentTimeMillis());
            if (application.getColorInfo() != null) {
                CodeLocatorApplicationInitializedListener.setColorInfo(application.getColorInfo());
            }
        }
        application.restoreAllStructInfo();
        if (application.getShowInfos() != null) {
            application.getShowInfos().sort(new Comparator<ShowInfo>() {
                @Override
                public int compare(ShowInfo o1, ShowInfo o2) {
                    return o1.getShowTime() < o2.getShowTime() ? 1 : (o1.getShowTime() == o2.getShowTime() ? 0 : -1);
                }
            });
        }
        if (application.getSchemaInfos() != null) {
            application.getSchemaInfos().sort(Comparator.comparing(SchemaInfo::getSchema));
        }
        sCurrentApkName = application.getPackageName();
        if (application.getActivity() != null) {
            restoreAllViewStructInfo(application.getActivity());
            final String startInfo = application.getActivity().getStartInfo();
            if (startInfo != null) {
                application.getActivity().setOpenActivityJumpInfo(JumpParser.getSingleJumpInfo(startInfo));
            }
        }
        restoreAllShowInfo(application.getShowInfos());
    }

    public static void restoreAllFileStructInfo(WFile wFile) {
        if (wFile == null) {
            return;
        }
        wFile.restoreAllFileStructInfo();
    }

    public static void sortFile(WFile wFile, boolean sortByName) {
        if (wFile == null) {
            return;
        }
        if (wFile.getChildren() != null) {
            if (sortByName) {
                wFile.getChildren().sort(Comparator.comparing(o -> o.getName().toLowerCase()));
            } else {
                wFile.getChildren().sort((o1, o2) -> {
                    final long l = o1.getLength() - o2.getLength();
                    if (l == 0) {
                        return 0;
                    } else if (l > 0) {
                        return 1;
                    } else {
                        return -1;
                    }
                });
            }
        }
        for (int i = 0; i < wFile.getChildCount(); i++) {
            sortFile(wFile.getChildAt(i), sortByName);
        }
    }

    public static HashMap<String, ExtraInfo> getViewAllTypeExtra(WView view, int actionType, boolean includeParent) {
        HashMap<String, ExtraInfo> hashMap = new HashMap<>();
        while (view != null) {
            final List<ExtraInfo> extraInfos = view.getExtraInfos();
            if (extraInfos != null && !extraInfos.isEmpty()) {
                for (ExtraInfo extra : extraInfos) {
                    if (extra == null || hashMap.get(extra.getTag()) != null) {
                        continue;
                    }
                    final ExtraAction extraAction = extra.getExtraAction();
                    if (extraAction == null) {
                        continue;
                    }
                    if ((extraAction.getActionType() & actionType) != 0) {
                        hashMap.put(extra.getTag(), extra);
                    }
                }
            }
            if (includeParent) {
                view = view.getParentView();
            } else {
                view = null;
            }
        }
        return hashMap;
    }

    public static HashMap<String, ExtraInfo> getViewAllTableTypeExtra(WView view) {
        if (view != null) {
            HashMap<String, ExtraInfo> hashMap = new HashMap<>();
            final List<ExtraInfo> extraInfos = view.getExtraInfos();
            if (extraInfos != null && !extraInfos.isEmpty()) {
                for (ExtraInfo extra : extraInfos) {
                    if (extra == null || hashMap.get(extra.getTag()) != null || extra.getShowType() == ExtraInfo.ShowType.EXTRA_TREE) {
                        continue;
                    }
                    final ExtraAction extraAction = extra.getExtraAction();
                    if (extraAction == null) {
                        continue;
                    }
                    hashMap.put(extra.getTag(), extra);
                }
            }
            return hashMap;
        }
        return null;
    }

    public static ExtraInfo getViewExtra(WView view, String extraTag) {
        while (view != null) {
            final ExtraInfo extraByTag = getExtraByTag(view.getExtraInfos(), extraTag);
            if (extraByTag != null) {
                return extraByTag;
            }
            view = view.getParentView();
        }
        return null;
    }

    public static ExtraInfo getExtraByTag(List<ExtraInfo> extraInfos, String extraTag) {
        if (extraInfos == null || extraInfos.isEmpty() || extraTag == null) {
            return null;
        }
        for (ExtraInfo extraInfo : extraInfos) {
            if (extraInfo == null) {
                continue;
            }
            if (extraTag.equals(extraInfo.getTag())) {
                return extraInfo;
            }
        }
        return null;
    }

    public static void restorePlatformInfo(WApplication application, String platFormInfo) {
        if (platFormInfo == null) {
            return;
        }
        final String[] splits = platFormInfo.split(";");
        if (splits == null || splits.length <= 0) {
            return;
        }
        for (String line : splits) {
            if (line.startsWith("V:")) {
                application.setSdkVersion(line.substring("V:".length()));
            } else if (line.startsWith("M:")) {
                application.setMinPluginVersion(line.substring("M:".length()));
            }
        }
    }

    private static void restoreAllViewStructInfo(WActivity activity) {
        if (activity == null || activity.getDecorViews() == null) {
            return;
        }
        final List<WView> decorViews = activity.getDecorViews();
        for (WView view : decorViews) {
            restoreAllViewStructInfo(view);
        }
    }

    private static void restoreAllViewStructInfo(WView view) {
        if (view == null) {
            return;
        }

        view.setXmlJumpInfo(JumpParser.getXmlJumpInfo(view.getXmlTag(), view.getIdStr()));
        view.setClickJumpInfo(JumpParser.getJumpInfo(view.getClickTag()));
        view.setTouchJumpInfo(JumpParser.getJumpInfo(view.getTouchTag()));
        view.setFindViewJumpInfo(JumpParser.getJumpInfo(view.getFindViewByIdTag()));

        for (int i = 0; i < view.getChildCount(); i++) {
            final WView childView = view.getChildAt(i);
            restoreAllViewStructInfo(childView);
        }
    }

    private static void restoreAllShowInfo(List<ShowInfo> showInfos) {
        if (showInfos == null || showInfos.isEmpty()) {
            return;
        }
        for (ShowInfo showInfo : showInfos) {
            showInfo.setJumpInfo(JumpParser.getSingleJumpInfo(showInfo.getShowInfo()));
        }
    }

    public static String getViewRealAlpha(WView view) {
        if (view == null) {
            return "";
        }
        float realAlpha = 1.0f;
        while (view != null) {
            realAlpha = realAlpha * view.getAlpha();
            view = view.getParentView();
        }
        return "" + realAlpha;
    }

    public static final int VISIBLE = 0x00000000;

    public static final int GONE = 0x00000008;

    public static WApplication buildApplicationForNoSDKDebug(Project project, String pkgName, String activityName, Client client) {
        try {
            List<ClientWindow> allWindows = null;
            WApplication wApplication = new WApplication();
            wApplication.setPackageName(pkgName);
            wApplication.setHasSDK(false);
            wApplication.setFromSdk(false);
            WActivity wActivity = new WActivity();
            wActivity.setClassName(activityName);
            wApplication.setActivity(wActivity);
            wActivity.setDecorViews(new ArrayList<>());
            allWindows = ClientWindow.getAll(client, 5, TimeUnit.SECONDS);
            if (allWindows == null) {
                return null;
            }
            List<Triple<WView, Integer, Integer>> viewList = new ArrayList<>();
            for (ClientWindow window : allWindows) {
                LayoutInspectorCaptureOptions options = new LayoutInspectorCaptureOptions();
                options.setVersion(ProtocolVersion.Version2);
                byte[] loadWindowData = window.loadWindowData(options, 10, TimeUnit.SECONDS);
                final ViewNodeV2Parser viewNodeV2Parser = new ViewNodeV2Parser();
                ViewNode viewNode = viewNodeV2Parser.parse(loadWindowData);
                try {
                    final WView view = convertToWView(viewNode);
                    final CodeLocatorViewNodeParser codeLocatorViewNodeParser = new CodeLocatorViewNodeParser();
                    final Pair<Point, Integer> pointIntegerPair = codeLocatorViewNodeParser.parseWindowInfo(loadWindowData, viewNodeV2Parser);
                    if (view != null) {
                        view.setLeftOffset(pointIntegerPair.getFirst().x);
                        view.setTopOffset(pointIntegerPair.getFirst().y);
                    }
                    viewList.add(new Triple<>(view, pointIntegerPair.getSecond(), getTotalViewCount(view)));
                } catch (Throwable ignore) {
                    Log.d("转换失败", ignore);
                }
            }
            viewList.sort((o1, o2) -> o1.getSecond().equals(o1.getSecond()) ? (o2.getThird() - o1.getThird()) : (o2.getSecond() - o1.getSecond()));
            for (Triple<WView, Integer, Integer> triple : viewList) {
                wActivity.getDecorViews().add(triple.getFirst());
            }
            try {
                final StringResponse response = DeviceManager.executeCmd(project,
                    new AdbCommand(new AdbAction(AdbCommand.ACTION.DUMPSYS, "activity " + activityName)),
                    StringResponse.class);
                final String dumpActivityStr = response.getData();
                final WApplication parser = new DumpInfoParser(dumpActivityStr).parser();
                if (parser != null && parser.getActivity() != null) {
                    wActivity.setFragments(parser.getActivity().getFragments());
                }
            } catch (Throwable t) {
                Log.e("setFragments for debug", t);
            }
            return wApplication;
        } catch (Throwable t) {
            Log.d("buildApplicationForNoSDKDebug error", t);
            return null;
        }
    }

    public static WApplication buildApplicationForNoSDKRelease(Project project, String pkgName, String activityName) {
        try {
            final StringResponse response = DeviceManager.executeCmd(project,
                new AdbCommand(new AdbAction(AdbCommand.ACTION.DUMPSYS, "activity " + activityName)),
                StringResponse.class);
            final String dumpActivityStr = response.getData();
            final WApplication parser = new DumpInfoParser(dumpActivityStr).parser();
            if (parser == null) {
                return null;
            }
            parser.setPackageName(pkgName);
            return parser;
        } catch (Throwable t) {
            Log.e("buildApplicationForNoSDKRelease error", t);
        }
        return null;
    }

    public static WView convertToWView(ViewNode node) {
        if (node == null) {
            return null;
        }
        WView view = new WView();

        view.setClassName(node.getProperty("__name__").getValue());
        view.setMemAddr(CodeLocatorUtils.toHexStr(Integer.valueOf(node.getProperty("__hash__").getValue())));
        view.setIdStr(node.getProperty("id").getValue());
        view.setLeft(Integer.valueOf(node.getProperty("left").getValue()));
        view.setRight(Integer.valueOf(node.getProperty("right").getValue()));
        view.setTop(Integer.valueOf(node.getProperty("top").getValue()));
        view.setBottom(Integer.valueOf(node.getProperty("bottom").getValue()));
        view.setScrollX(Integer.valueOf(node.getProperty("scrollX").getValue()));
        view.setScrollY(Integer.valueOf(node.getProperty("scrollY").getValue()));
        view.setScaleX(Float.valueOf(node.getProperty("scaleX").getValue()));
        view.setScaleY(Float.valueOf(node.getProperty("scaleY").getValue()));
        view.setPaddingLeft(Integer.valueOf(node.getProperty("paddingLeft").getValue()));
        view.setPaddingRight(Integer.valueOf(node.getProperty("paddingRight").getValue()));
        view.setPaddingTop(Integer.valueOf(node.getProperty("paddingTop").getValue()));
        view.setPaddingBottom(Integer.valueOf(node.getProperty("paddingBottom").getValue()));
        view.setTranslationX(Float.valueOf(node.getProperty("translationX").getValue()));
        view.setTranslationY(Float.valueOf(node.getProperty("translationY").getValue()));
        view.setAlpha(Float.valueOf(node.getProperty("alpha").getValue()));

        final int visibility = Integer.valueOf(node.getProperty("visibility").getValue());
        view.setVisibility(visibility == VISIBLE ? 'V' : (visibility == GONE ? 'G' : 'I'));

        view.setEnabled(Boolean.valueOf(node.getProperty("enabled").getValue()));
        view.setClickable(Boolean.valueOf(node.getProperty("clickable").getValue()));
        view.setFocusable(Boolean.valueOf(node.getProperty("isFocused").getValue()));
        view.setPressed(Boolean.valueOf(node.getProperty("pressed").getValue()));
        view.setSelected(Boolean.valueOf(node.getProperty("selected").getValue()));
        if (node.getProperty("leftMargin") != null) {
            view.setMarginLeft(Integer.valueOf(node.getProperty("leftMargin").getValue()));
        }
        if (node.getProperty("topMargin") != null) {
            view.setMarginTop(Integer.valueOf(node.getProperty("topMargin").getValue()));
        }
        if (node.getProperty("rightMargin") != null) {
            view.setMarginRight(Integer.valueOf(node.getProperty("rightMargin").getValue()));
        }
        if (node.getProperty("bottomMargin") != null) {
            view.setMarginBottom(Integer.valueOf(node.getProperty("bottomMargin").getValue()));
        }
        view.setLayoutWidth(Integer.valueOf(node.getProperty("width").getValue()));
        view.setLayoutHeight(Integer.valueOf(node.getProperty("height").getValue()));
        final String className = view.getClassName();
        if (className != null) {
            if (className.contains("TextView")) {
                view.setType(WView.Type.TYPE_TEXT);
                if (node.getProperty("text") != null) {
                    view.setText(node.getProperty("text").getValue());
                }
                if (node.getProperty("curTextColor") != null) {
                    view.setTextColor(CodeLocatorUtils.toHexStr(Integer.valueOf(node.getProperty("curTextColor").getValue())));
                }
                if (node.getProperty("scaledTextSize") != null) {
                    view.setTextSize(Float.valueOf(node.getProperty("scaledTextSize").getValue()));
                }
            } else if (className.contains("ImageView")) {
                view.setType(WView.Type.TYPE_IMAGE);
            } else if (className.contains("Linear")) {
                view.setType(WView.Type.TYPE_LINEAR);
            } else if (className.contains("Relative")) {
                view.setType(WView.Type.TYPE_RELATIVE);
            } else if (className.contains("FrameLayout")) {
                view.setType(WView.Type.TYPE_FRAME);
            } else if (className.contains("Decor")) {
                view.setType(WView.Type.TYPE_FRAME);
            }
        }
        if (node.getChildCount() > 0) {
            ArrayList<WView> child = new ArrayList<>(node.getChildCount());
            for (int i = 0; i < node.getChildCount(); i++) {
                child.add(convertToWView(node.getChildAt(i)));
            }
            view.setChildren(child);
        }
        return view;
    }

    public static int getTotalViewCount(WView view) {
        if (view == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < view.getChildCount(); i++) {
            count += getTotalViewCount(view.getChildAt(i));
        }
        return count + 1;
    }

}
