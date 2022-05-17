package com.bytedance.tools.codelocator.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtils {

    public static Gson sGson = new GsonBuilder()
        .serializeSpecialFloatingPointValues()
        .create();

}
