package com.jfeat.jar.dependency.model;

public class ChecksumModel {
    private String dependency;
    private long checksum;

    public String getDependency() {
        return dependency;
    }

    public void setDependency(String dependency) {
        this.dependency = dependency;
    }

    public long getChecksum() {
        return checksum;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }
}
