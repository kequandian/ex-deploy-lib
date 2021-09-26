package com.jfeat.jar.dependency.model;

import com.alibaba.fastjson.JSONObject;

public class JarModel {
    private String jar;
    private long checksum;

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    public long getChecksum() {
        return checksum;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
