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
        HashSet<String> resultPinyinSetTmp = new HashSet<>();
        HashSet<String> tmpPinyinSet = new HashSet<>();
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
                        if (resultPinyinSet.isEmpty()) {
                            resultPinyinSetTmp.add(charPinyin);
                        } else {
                            for (String currentPinyinStr : resultPinyinSet) {
                                resultPinyinSetTmp.add(currentPinyinStr + charPinyin);
                            }
                        }
                    }
                    resultPinyinSet.clear();
                    resultPinyinSet.addAll(resultPinyinSetTmp);
                    resultPinyinSetTmp.clear();
                } else {
                    final String currentCharStr = String.valueOf(str.charAt(i)).toLowerCase();
                    if (resultPinyinSet.isEmpty()) {
                        resultPinyinSetTmp.add(currentCharStr);
                    } else {
                        for (String currentPinyinStr : resultPinyinSet) {
                            resultPinyinSetTmp.add(currentPinyinStr + currentCharStr);
                        }
                    }
                    resultPinyinSet.clear();
                    resultPinyinSet.addAll(resultPinyinSetTmp);
                    resultPinyinSetTmp.clear();
                }
            } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
                badHanyuPinyinOutputFormatCombination.printStackTrace();
            }
        }
        return resultPinyinSet;
    }

}
