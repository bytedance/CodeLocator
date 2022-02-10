package com.bytedance.tools.codelocator.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PinyinUtils {

    static HanyuPinyinOutputFormat hanyuPinyinOutputFormat = new HanyuPinyinOutputFormat();

    static {
        hanyuPinyinOutputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    public static Set<String> getAllPinyinStr(String str) {
        if (str == null || str.trim().isEmpty()) {
            return Collections.emptySet();
        }
        HashSet<String> resultPinyinSet = new HashSet<>();
        HashSet<String> tmpPinyinSet = new HashSet<>();
        StringBuilder resultPinyinBuilder = new StringBuilder("");
        for (int i = 0; i < str.length(); i++) {
            tmpPinyinSet.clear();
            try {
                final String[] strings = PinyinHelper.toHanyuPinyinStringArray(str.charAt(i), hanyuPinyinOutputFormat);
                if (strings != null && strings.length > 0) {
                    for (String pinyin : strings) {
                        tmpPinyinSet.add(pinyin);
                    }
                }
                if (tmpPinyinSet.size() > 0) {
                    for (String charPinyin : tmpPinyinSet) {
                        resultPinyinBuilder.append(charPinyin);
                    }
                } else {
                    final String currentCharStr = String.valueOf(str.charAt(i)).toLowerCase();
                    resultPinyinBuilder.append(currentCharStr);
                }
            } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
                badHanyuPinyinOutputFormatCombination.printStackTrace();
            }
        }
        resultPinyinSet.add(resultPinyinBuilder.toString());
        return resultPinyinSet;
    }

}
