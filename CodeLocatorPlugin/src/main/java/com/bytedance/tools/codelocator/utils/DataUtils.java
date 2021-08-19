package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.model.*;
import com.bytedance.tools.codelocator.parser.JumpParser;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class DataUtils {

    public static void restoreAllStructInfo(WApplication application) {
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
        FileUtils.sPkgName = application.getPackageName();
        if (application.getActivity() != null) {
            restoreAllViewStructInfo(application.getActivity().getDecorView());
            final String startInfo = application.getActivity().getStartInfo();
            if (startInfo != null) {
                application.getActivity().setOpenActivityJumpInfo(JumpParser.getSingleJumpInfo(startInfo));
            }
        }
        restoreAllShowInfo(application.getShowInfos());
    }

    public static void restoreAllStructInfo(WFile wFile) {
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

}
