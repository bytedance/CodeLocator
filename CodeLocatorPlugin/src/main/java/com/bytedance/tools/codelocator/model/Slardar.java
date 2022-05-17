package com.bytedance.tools.codelocator.model;

public class Slardar {

    private String _id;
    private String msg;
    private String url;
    private String user;
    private String status;
    private String slardarId;
    private long time;

    public void set_id(String _id) {
        this._id = _id;
    }

    public String get_id() {
        return _id;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setSlardarId(String slardarId) {
        this.slardarId = slardarId;
    }

    public String getSlardarId() {
        return slardarId;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

}