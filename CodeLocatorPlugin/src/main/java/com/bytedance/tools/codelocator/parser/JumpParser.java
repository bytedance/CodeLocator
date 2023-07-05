package com.bytedance.tools.codelocator.parser;

import com.bytedance.tools.codelocator.model.JumpInfo;
import com.bytedance.tools.codelocator.utils.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JumpParser {

    public static List<JumpInfo> getJumpInfo(String jumpStr) {
        if (jumpStr == null || jumpStr.trim().isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        String substring = jumpStr;
        final String[] split = substring.split("\\|");
        List<JumpInfo> jumpInfos = new ArrayList<>(split.length);
        for (int i = 0; i < split.length; i++) {
            final JumpInfo singleJumpInfo = getSingleJumpInfo(split[i]);
            if (singleJumpInfo != null) {
                jumpInfos.add(singleJumpInfo);
            }
        }
        return jumpInfos;
    }


    public static JumpInfo getXmlJumpInfo(String jumpStr, String id) {
        if (jumpStr == null || jumpStr.trim().isEmpty()) {
            return null;
        }
        String xmlName = jumpStr;
        final JumpInfo xmlJumpInfo = new JumpInfo(xmlName);
        if (id != null) {
            if (id.startsWith("app:")) {
                xmlJumpInfo.setId(id.substring("app:".length()));
            } else if (id.startsWith("android:")) {
                xmlJumpInfo.setId(id.substring("android:".length()));
            } else if (id.startsWith("id/")) {
                xmlJumpInfo.setId(id.substring("id/".length()));
            }
        }
        return xmlJumpInfo;
    }

    public static JumpInfo getSingleJumpInfo(String jumpStr) {
        if (jumpStr == null || jumpStr.trim().isEmpty()) {
            return null;
        }
        final String[] split = jumpStr.split(":");
        if (split.length < 2) {
            return null;
        }
        JumpInfo jumpInfo = new JumpInfo(split[0]);
        if (split[1].startsWith("id/")) {
            jumpInfo.setId(split[1].substring("id/".length()));
        } else if (split[1].startsWith("bind_id/")) {
            jumpInfo.setIsViewBinding(true);
            jumpInfo.setId(split[1].substring("bind_id/".length()));
            try {
                jumpInfo.setLineCount(Integer.valueOf(split[2]));
            } catch (Exception e) {
                Log.e("Jump Info 解析失败, Str: " + jumpStr);
            }
        } else {
            try {
                jumpInfo.setLineCount(Integer.valueOf(split[1]));
            } catch (Exception e) {
                Log.e("Jump Info 解析失败, Str: " + jumpStr);
                return null;
            }
        }
        return jumpInfo;
    }
}
