package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.model.WApplication;
import com.bytedance.tools.codelocator.model.WView;

import java.awt.*;

public class UIUtils {

    public static int dip2Px(float density, float dipValue) {
        float offset = (dipValue >= 0 ? 0.5f : -0.5f);
        return (int) (dipValue * density + offset);
    }

    public static int px2dip(float density, int pxValue) {
        float offset = pxValue >= 0 ? 0.5f : -0.5f;
        return (int) (pxValue / Math.max(1, density) + offset);
    }

    public static float px2dipFloat(float density, int pxValue) {
        float offset = pxValue >= 0 ? 0.5f : -0.5f;
        final float dpFloat = pxValue / density;
        if (Math.abs(((int) dpFloat) - dpFloat) < 0.0001) {
            return (int) dpFloat;
        }
        if (dip2Px(density, (int) dpFloat) == pxValue) {
            return ((int) dpFloat);
        } else if (dip2Px(density, ((int) dpFloat) + offset) == pxValue) {
            return ((int) dpFloat) + offset;
        } else if (dip2Px(density, ((int) dpFloat) + offset * 2) == pxValue) {
            return ((int) dpFloat) + (offset * 2);
        }
        return ((int) dpFloat) + offset;
    }

    public static String getSizeStr(Float valueInPx, WApplication application) {
        if (valueInPx == null) {
            return "";
        }
        if (application == null) {
            return valueInPx.toString();
        }
        if (valueInPx != 0) {
            return valueInPx + "px (" + UIUtils.px2dip(application.getDensity(), valueInPx.intValue()) + "dp)";
        }
        return valueInPx + "px";
    }

    public static String getSizeStr(Integer valueInPx, WApplication application) {
        if (valueInPx == null) {
            return "";
        }
        if (application == null) {
            return valueInPx.toString();
        }
        if (valueInPx != 0) {
            return valueInPx + "px (" + UIUtils.px2dip(application.getDensity(), valueInPx) + "dp)";
        }
        return valueInPx + "px";
    }

    public static String getCommonStr(String paddingOrMargin, WApplication application) {
        if (paddingOrMargin == null) {
            return "";
        }
        if (!paddingOrMargin.isEmpty() && !"0, 0, 0, 0".equals(paddingOrMargin)) {
            final String[] split = paddingOrMargin.replace(" ", "").split(",");
            StringBuilder sb = new StringBuilder();
            sb.append(paddingOrMargin.replace(",", "px, "));
            sb.append("px");
            for (int i = 0; application != null && i < split.length; i++) {
                final float dp = UIUtils.px2dip(application.getDensity(), Integer.valueOf(split[i]));
                if (i != 0) {
                    sb.append(", ");
                } else {
                    sb.append(" (");
                }
                sb.append(dp);
                sb.append("dp");
                if (i == split.length - 1) {
                    sb.append(")");
                }
            }
            return sb.toString();
        }
        return "0px, 0px, 0px, 0px";
    }

    public static String getSizeStr(WView view, WApplication appInfo) {
        if (view == null) {
            return "";
        }
        int width = view.getRealWidth();
        int height = view.getRealHeight();
        StringBuilder sb = new StringBuilder();
        sb.append(width);
        sb.append("px, ");
        sb.append(height);
        sb.append("px");
        if (appInfo != null && (width != 0 || height != 0)) {
            sb.append(" (");
            sb.append(UIUtils.px2dip(appInfo.getDensity(), width));
            sb.append("dp, ");
            sb.append(UIUtils.px2dip(appInfo.getDensity(), height));
            sb.append("dp)");
        }
        return sb.toString();
    }

    public static String getPositionStr(WView view) {
        if (view == null) {
            return "";
        }
        return "Screen: [" + view.getDrawLeft() + "," + view.getDrawTop() + "][" + view.getDrawRight() + "," + view.getDrawBottom() + "] Parent: " + getParentPositionStr(view);
    }

    public static String getParentPositionStr(WView view) {
        if (view.getParentView() == null) {
            return "[" + view.getDrawLeft() + "," + view.getDrawTop() + "][" + view.getDrawRight() + "," + view.getBottom() + "]";
        } else {
            return "[" + (view.getDrawLeft() - view.getParentView().getDrawLeft()) + "," + (view.getDrawTop() - view.getParentView().getDrawTop()) + "]" +
                "[" + (view.getDrawRight() - view.getParentView().getDrawLeft()) + "," + (view.getDrawBottom() - view.getParentView().getDrawTop()) + "]";
        }
    }

    public static String getLayoutStr(String layoutStr, WApplication application) {
        if (layoutStr == null) {
            return "";
        }
        if (application == null) {
            return layoutStr.replace("-2", "wrap_content").replace("-1", "match_parent");
        }

        final String[] split = layoutStr.replace(" ", "").split(",");
        int width = Integer.valueOf(split[0].trim());
        int height = Integer.valueOf(split[1].trim());

        StringBuilder sb = new StringBuilder();
        appendLayoutStr(width, sb, null);
        sb.append(", ");
        appendLayoutStr(height, sb, null);

        if (width > 0 || height > 0) {
            sb.append(" (");
            appendLayoutStr(width, sb, application);
            sb.append(", ");
            appendLayoutStr(height, sb, application);
            sb.append(")");
        }
        return sb.toString();
    }

    private static void appendLayoutStr(int size, StringBuilder sb, WApplication application) {
        if (size >= 0) {
            if (application == null) {
                sb.append(size);
                sb.append("px");
            } else {
                sb.append(UIUtils.px2dip(application.getDensity(), size));
                sb.append("dp");
            }
        } else if (size == -1) {
            sb.append("match_parent");
        }
        if (size == -2) {
            sb.append("wrap_content");
        }
    }

    public static String getMatchWidthStr(String sourceStr, FontMetrics fontMetrics, int maxWidth) {
        if (sourceStr == null || sourceStr.isEmpty()) {
            return "";
        }
        int width = fontMetrics.stringWidth(sourceStr);
        if (width <= maxWidth) {
            return sourceStr;
        }
        int index = sourceStr.length() - 2;
        while (index > 0 && fontMetrics.stringWidth(sourceStr.substring(0, index)) > maxWidth) {
            index--;
        }
        return sourceStr.substring(0, index) + " ...";
    }
}
