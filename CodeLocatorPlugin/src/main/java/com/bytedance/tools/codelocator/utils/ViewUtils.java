package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.model.WView;
import com.bytedance.tools.codelocator.panels.ScreenPanel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.annotation.Nullable;

public class ViewUtils {

    public static @Nullable
    WView findClickedView(WView rootView, int clickX, int clickY, boolean justFindClickableView) {
        WView clickedView = null;
        ArrayList<WView> clickViews = new ArrayList<>();
        findClickedView(rootView, clickX, clickY, clickViews, false);
        clickViews.sort((o1, o2) -> {
            if (justFindClickableView) {
                if (o2.getZIndex().equals(o1.getZIndex())) {
                    return o2.getArea() - o1.getArea();
                }
                return o2.getZIndex().compareTo(o1.getZIndex());
            } else {
                return o1.getArea() - o2.getArea();
            }
        });
        if (justFindClickableView) {
            for (int i = 0; i < clickViews.size(); i++) {
                if (clickViews.get(i).isClickable()) {
                    if (clickedView != null) {
                        if (clickedView.getArea() < clickViews.get(i).getArea()) {
                            break;
                        }
                    }
                    clickedView = clickViews.get(i);
                    break;
                } else if (clickedView == null || clickedView.getArea() > clickViews.get(i).getArea()) {
                    clickedView = clickViews.get(i);
                }
            }
        } else {
            if (clickViews.size() > 0) {
                clickedView = clickViews.get(0);
            }
        }
        tryFindViewClickInfo(clickedView);
        return clickedView;
    }

    public static @Nullable
    List<WView> findClickedViewList(WView rootView, int clickX, int clickY) {
        ArrayList<WView> clickViews = new ArrayList<>();
        findClickedView(rootView, clickX, clickY, clickViews, true);
        clickViews.sort(Comparator.comparing(WView::getZIndex));
        return clickViews;
    }

    public static WView findViewUniqueIdParent(WView parentView, WView view) {
        if (view == null) {
            return null;
        }
        if (parentView == null) {
            parentView = view.getActivity().getDecorView();
        }
        while (view != null) {
            if (viewIdIsUnique(parentView, view.getIdStr())) {
                return view;
            }
            if (view == parentView) {
                return null;
            }
            view = view.getParentView();
        }
        return view;
    }

    public static boolean viewIdIsUnique(WView rootView, String viewIdStr) {
        if (rootView == null || viewIdStr == null || viewIdStr.isEmpty()) {
            return false;
        }
        int findCount = 0;
        LinkedList<WView> views = new LinkedList<>();
        views.add(rootView);
        while (!views.isEmpty()) {
            int levelCount = views.size();
            for (int i = 0; i < levelCount; i++) {
                final WView view = views.getFirst();
                views.remove(view);
                if (viewIdStr.equals(view.getIdStr())) {
                    findCount++;
                }
                for (int index = 0; index < view.getChildCount(); index++) {
                    views.add(view.getChildAt(index));
                }
            }
            if (findCount > 1) {
                return false;
            }
        }
        return true;
    }

    public static @Nullable
    List<WView> findViewList(@Nullable WView rootView, @Nullable List<String> viewIdList) {
        if (rootView == null || viewIdList == null || viewIdList.isEmpty()) {
            return null;
        }
        List<WView> clickViewList = new LinkedList<>();
        Stack<WView> searchStack = new Stack<>();
        searchStack.add(rootView);
        boolean find = false;
        for (int i = 0; i < viewIdList.size(); i++) {
            find = false;
            while (!searchStack.isEmpty()) {
                final WView pop = searchStack.pop();
                if (pop.getMemAddr().equals(viewIdList.get(i))) {
                    find = true;
                    clickViewList.add(pop);
                    searchStack.clear();
                    for (int j = 0; j < pop.getChildCount(); j++) {
                        searchStack.push(pop.getChildAt(j));
                    }
                    break;
                }
            }
            if (!find) {
                clickViewList.clear();
                return clickViewList;
            }
        }
        return clickViewList;
    }

    public static void tryFindViewClickInfo(WView clickedView) {
        if (clickedView == null) {
            return;
        }
        int count = 0;
        WView parent = clickedView;
        String clickTag = clickedView.getClickTag();
        while (clickTag == null && count < 10) {
            parent = parent.getParentView();
            if (parent == null) {
                break;
            }
            clickTag = parent.getClickTag();
            count++;
            if (clickTag != null && parent.getArea() - clickedView.getArea() < 300 * 300) {
                clickedView.setClickTag(clickTag);
                clickedView.setClickJumpInfo(parent.getClickJumpInfo());
                break;
            }
        }
        tryFindViewTouchInfo(clickedView);
    }

    public static void tryFindViewTouchInfo(WView touchView) {
        if (touchView == null) {
            return;
        }
        int count = 0;
        WView parent = touchView;
        String touchTag = touchView.getTouchTag();
        while (touchTag == null && count < 10) {
            parent = parent.getParentView();
            if (parent == null) {
                break;
            }
            touchTag = parent.getTouchTag();
            count++;
            if (touchTag != null && parent.getArea() - touchView.getArea() < 300 * 300) {
                touchView.setTouchTag(touchTag);
                touchView.setTouchJumpInfo(parent.getTouchJumpInfo());
                break;
            }
        }
    }

    private static boolean findClickedView(WView view, int clickX, int clickY, ArrayList<WView> clickViews, boolean findJustClickView) {
        if (view == null) {
            return false;
        }
        if ('V' == view.getVisibility() && view.contains(clickX, clickY)) {
            boolean hasClickChild = false;
            for (int i = view.getChildCount() - 1; i > -1; i--) {
                hasClickChild = findClickedView(view.getChildAt(i), clickX, clickY, clickViews, findJustClickView) || hasClickChild;
            }
            if (!hasClickChild) {
                if (view.isClickable()) {
                    clickViews.add(0, view);
                } else if (!findJustClickView) {
                    clickViews.add(view);
                }
            }
            return hasClickChild;
        }
        return false;
    }

    public static List<WView> filterChildView(WView view, int filterMode) {
        if (view == null) {
            return null;
        }
        LinkedList<WView> filterViews = new LinkedList<>();
        if (filterMode == ScreenPanel.FILTER_GONE) {
            filterChildViewByVisible(view, filterViews, 'G');
        } else if (filterMode == ScreenPanel.FILTER_INVISIBLE) {
            filterChildViewByVisible(view, filterViews, 'I');
        } else {
            filterChildViewByOverDraw(view, view, filterViews);
        }
        return filterViews;
    }

    private static void filterChildViewByVisible(WView view, List<WView> filterViews, char visibility) {
        if (view == null) {
            return;
        }
        if (view.getVisibility() == visibility) {
            filterViews.add(view);
        }
        for (int i = 0; i < view.getChildCount(); i++) {
            filterChildViewByVisible(view.getChildAt(i), filterViews, visibility);
        }
    }

    private static void filterChildViewByOverDraw(WView rootView, WView view, List<WView> filterViews) {
        if (view == null) {
            return;
        }
        boolean isOverDraw = false;
        if (view.getVisibility() == 'V' && view.getBackgroundColor() != null && !view.getBackgroundColor().isEmpty()) {
            WView tmpView = view.getParentView();
            while (tmpView != null) {
                if (tmpView.getVisibility() != 'V') {
                    break;
                }
                if (tmpView.getBackgroundColor() != null && !tmpView.getBackgroundColor().isEmpty()) {
                    isOverDraw = true;
                    break;
                }
                tmpView = tmpView.getParentView();
                if (rootView == tmpView) {
                    break;
                }
            }
        }
        if (isOverDraw) {
            filterViews.add(view);
        }
        for (int i = 0; i < view.getChildCount(); i++) {
            filterChildViewByOverDraw(rootView, view.getChildAt(i), filterViews);
        }
    }

    public static int getViewDeep(WView view) {
        int count = 0;
        while (view != null) {
            count++;
            if (view.getClassName().endsWith(".DecorView") || view.getClassName().endsWith("$PopupDecorView")) {
                break;
            }
            view = view.getParentView();
        }
        return count;
    }
}
