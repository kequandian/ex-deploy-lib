package com.jfeat.am.jar.dep.request;

public class JarRequest {
    /**
     * the dir jar locates
     */
    private String dir;
    /**
     * JAR
     */
    private String jar;
    /**
     * the content which zip entry name contains
     */
    private String pattern;
    /**
     * the target path to extra to
     */
    private String target;


    public String getTarget() {
        return target==null?"":target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    public String getDir() {
        return dir==null?"":dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
