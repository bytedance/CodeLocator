package com.bytedance.tools.codelocator.utils;

public class CodeLocatorConstants {

    public interface R {

        interface id {
            int codeLocator_background_tag_id = 0x7f079990;
            int codeLocator_background_tag_info = 0x7f079991;
            int codeLocator_drawable_tag_id = 0x7f079992;
            int codeLocator_drawable_tag_info = 0x7f079993;
            int codeLocator_findviewbyId_tag_id = 0x7f079994;
            int codeLocator_onclick_tag_id = 0x7f079995;
            int codeLocator_ontouch_tag_id = 0x7f079996;
            int codeLocator_view_extra = 0x7f079997;
            int codeLocator_viewholder_adapter_tag_id = 0x7f079998;
            int codeLocator_viewholder_id = 0x7f079999;
            int codeLocator_viewholder_tag_id = 0x7f07999A;
            int codeLocator_xml_tag_id = 0x7f07999B;
        }

    }

    public static final int USE_TRANS_FILE_SDK_VERSION = 30;

    public static final String ACTION_OPERATE_CUSTOM_FILE = "com.bytedance.tools.codelocator.action_operate_custom_file";

    public static final String ACTION_DEBUG_FILE_INFO = "com.bytedance.tools.codelocator.action_debug_file_info";

    public static final String ACTION_DEBUG_FILE_OPERATE = "com.bytedance.tools.codelocator.action_debug_file_operate";

    public static final String ACTION_DEBUG_LAYOUT_INFO = "com.bytedance.tools.codelocator.action_debug_layout_info";

    public static final String ACTION_CHANGE_VIEW_INFO = "com.bytedance.tools.codelocator.action_change_view_info";

    public static final String ACTION_PROCESS_CONFIG_LIST = "com.bytedance.tools.codelocator.action_process_config_list";

    public static final String ACTION_PROCESS_SCHEMA = "com.bytedance.tools.codelocator.action_process_schema";

    public static final String ACTION_GET_TOUCH_VIEW = "com.bytedance.tools.codelocator.action_get_touch_view";

    public static final String ACTION_MOCK_TOUCH_VIEW = "com.bytedance.tools.codelocator.action_mock_touch_view";

    public static final String ACTION_USE_TOOLS_INFO = "com.bytedance.tools.codelocator.action_use_tools_info";

    public static final String ACTION_CONFIG_SDK = "com.bytedance.tools.codelocator.action_config_sdk";

    public static final String ACTIVITY_START_STACK_INFO = "codeLocator_activity_start_stack_info";

    public static final String KEY_SHELL_ARGS = "codeLocator_shell_args";

    public static final String KEY_CHANGE_VIEW = "codeLocator_change_view";

    public static final String KEY_MOCK_CLICK_X = "codeLocator_mock_click_x";

    public static final String KEY_MOCK_CLICK_Y = "codeLocator_mock_click_y";

    public static final String KEY_CODELOCATOR_ACTION = "codeLocator_action";

    public static final String KEY_PROCESS_SOURCE_FILE_PATH = "codeLocator_process_source_file_path";

    public static final String KEY_PROCESS_TARGET_FILE_PATH = "codeLocator_process_target_file_path";

    public static final String KEY_PROCESS_FILE_OPERATE = "codeLocator_process_file_operate";

    public static final String KEY_ACTION_PULL = "pull";

    public static final String KEY_ACTION_MOVE = "move";

    public static final String KEY_ACTION_GET = "get";

    public static final String KEY_ACTION_SET = "set";

    public static final String KEY_ACTION_ADD = "add";

    public static final String KEY_ACTION_CLEAR = "clear";

    public static final String KEY_ACTION_DELETE = "delete";

    public static final String KEY_CONFIG_TYPE = "config_type";

    public static final String KEY_CUSTOM_TAG = "custom_tag";

    public static final String KEY_SCHEMA = "codeLocator_schema";

    public static final String KEY_DATA = "codeLocator_data";

    public static final String KEY_SAVE_TO_FILE = "codeLocator_save_to_file";

    public static final String KEY_ASYNC = "codeLocator_save_async";

    public static final String KEY_NEED_COLOR = "codeLocator_need_color";

    public static final String KEY_STOP_ALL_ANIM = "codeLocator_stop_all_anim";

    public static final String KEY_TOOLS_COMMAND = "codeLocator_tools_command";

    public static final String BASE_DIR_NAME = "codeLocator";

    public static final String BASE_DIR_PATH = "/sdcard/" + BASE_DIR_NAME;

    public static final String BASE_TMP_DIR_PATH = "/data/local/tmp/" + BASE_DIR_NAME;

    public static final String TYPE_ACTIVITY_IGNORE = "activity_ignore";

    public static final String TYPE_VIEW_IGNORE = "view_ignore";

    public static final String TYPE_POPUP_IGNORE = "popup_ignore";

    public static final String TYPE_DIALOG_IGNORE = "dialog_ignore";

    public static final String TYPE_TOAST_IGNORE = "toast_ignore";

    public static final String TYPE_ENABLE_CODELOCATOR = "enable_codelocator";

    public static final String TYPE_ENABLE_CODELOCATOR_LANCET = "enable_codelocator_lancet";

    public static final String COMMAND_UPDATE_ACTIVITY = "command_update_activity";

    public static final String TMP_DATA_FILE_NAME = "codeLocator_data.txt";

    public static final String TMP_TRANS_DATA_DIR_PATH = "/sdcard/Download/";

    public static final String TMP_TRANS_DATA_FILE_PATH = TMP_TRANS_DATA_DIR_PATH + TMP_DATA_FILE_NAME;

    public static final String TMP_IMAGE_FILE_NAME = "codeLocator_image.png";

    public static final String TMP_TRANS_IMAGE_FILE_PATH = TMP_TRANS_DATA_DIR_PATH + TMP_IMAGE_FILE_NAME;

    public interface OperateType {

        String VIEW = "V";

        String ACTIVITY = "A";

        String FRAGMENT = "F";

        String APPLICATION = "P";

    }

    public interface EditType {

        String PADDING = "P";

        String MARGIN = "M";

        String BACKGROUND = "B";

        String VIEW_FLAG = "VF";

        String LAYOUT_PARAMS = "LP";

        String TRANSLATION_XY = "TXY";

        String SCROLL_XY = "SXY";

        String SCALE_XY = "SCXY";

        String PIVOT_XY = "PXY";

        String TEXT = "T";

        String TEXT_COLOR = "TC";

        String TEXT_SIZE = "TS";

        String LINE_SPACE = "LS";

        String SHADOW_XY = "SA";

        String SHADOW_RADIUS = "SR";

        String SHADOW_COLOR = "SC";

        String MINIMUM_HEIGHT = "MH";

        String MINIMUM_WIDTH = "MW";

        String ALPHA = "A";

        String VIEW_BITMAP = "VB";

        String DRAW_LAYER_BITMAP = "DLB";

        String ONLY_FOREGROUND = "OF";

        String ONLY_BACKGROUND = "OB";

        String GET_VIEW_DATA = "GVD";

        String SET_VIEW_DATA = "SVD";

        String GET_VIEW_CLASS_INFO = "GVCI";

        String GET_INTENT = "GI";

        String CLOSE_ACTIVITY = "CA";

        String INVOKE = "IK";

        String IGNORE = "X";

        String FECTH_URL = "FU";

        String ASYNC_BROADCAST = "AB";

        String GET_CLASS_INFO = "GCI";
    }

    public interface ResultKey {

        String SPLIT = ":";

        String ERROR = "Error";

        String DATA = "Data";

        String PKG_NAME = "PN";

        String TARGET_CLASS = "TC";

        String FILE_PATH = "FP";

        String STACK_TRACE = "ST";

    }

    public interface Error {

        String NO_CURRENT_ACTIVITY = "no_current_activity";

        String VIEW_NOT_FOUND = "view_not_found";

        String ACTIVITY_NOT_FOUND = "activity_not_found";

        String FRAGMENT_NOT_FOUND = "fragment_not_found";

        String BUNDLE_IS_NULL = "bundle_is_null";

        String FILE_NOT_EXIST = "file_not_exist";

        String EDIT_CONTENT_ERROR = "edit_content_error";

        String DELETE_FILE_FAILED = "delete_file_failed";

        String OPERATE_NOT_SUPPORT = "operate_not_support";

        String ARGS_EMPTY = "args_empty";

        String NOT_UI_THREAD = "not_ui_thread";

        String ERROR_WITH_STACK_TRACE = "error_with_stack_trace";

    }

}
