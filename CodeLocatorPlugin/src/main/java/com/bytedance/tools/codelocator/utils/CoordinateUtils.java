package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.model.WApplication;

import java.awt.*;

public class CoordinateUtils {

    public static final int SCALE_TO_LAND_HEIGHT = 300;

    public static final int PANEL_WIDTH = 420;

    public static int TABLE_RIGHT_MARGIN = 5;

    public static int DEFAULT_BORDER = 10;

    public static int getDefaultHeight() {
        return Toolkit.getDefaultToolkit().getScreenSize().getHeight() >= 800 ? 800 : 680;
    }

    public static int convertPanelXToPhoneX(WApplication application, int panelX, float scale, int transX) {
        if (application == null) {
            return 0;
        }
        int x = panelX;
        if (x < 0 || x > application.getOverrideScreenWidth()) {
            return -1;
        }
        return (int) (application.getPanelToPhoneRatio() * (x / scale - transX));
    }

    public static int convertPhoneXToPanelX(WApplication application, int phoneX) {
        if (application == null) {
            return 0;
        }
        return (int) (phoneX / application.getPanelToPhoneRatio());
    }

    public static int convertPanelYToPhoneY(WApplication application, int panelY, float scale, int transY) {
        if (application == null) {
            return 0;
        }
        int y = panelY;
        if (y < 0 || y > application.getPanelHeight()) {
            return -1;
        }
        return (int) (application.getPanelToPhoneRatio() * (y / scale - transY));
    }

    public static int convertPhoneYToPanelY(WApplication application, int phoneY) {
        if (application == null) {
            return 0;
        }
        return (int) (phoneY / application.getPanelToPhoneRatio());
    }

    public static int convertPanelDistanceToPhoneDistance(WApplication application, int panelDistance) {
        return (int) (panelDistance * application.getPanelToPhoneRatio());
    }

    public static int convertPhoneDistanceToPanelDistance(WApplication application, int phoneDistance) {
        if (application == null) {
            return 0;
        }
        return (int) (phoneDistance / application.getPanelToPhoneRatio());
    }
}
