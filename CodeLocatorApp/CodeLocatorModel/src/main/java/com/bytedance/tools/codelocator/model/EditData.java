package com.bytedance.tools.codelocator.model;

import com.google.gson.annotations.SerializedName;

public class EditData {

    @SerializedName("d7")
    public String type;

    @SerializedName("d8")
    public String args;

    public EditData() {
    }

    public EditData(String type, String args) {
        this.type = type;
        this.args = args;
    }

}