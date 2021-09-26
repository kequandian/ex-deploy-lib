package com.jfeat.am.jar.dep.request;

public class JarRequest {
    /**
     * 子目录
     */
    private String dir;
    private String jar;
    private String pattern;
    private String target;

    private String javaclass;


    public String getTarget() {
        return target;
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

    public String getJavaclass() {
        return javaclass;
    }

    public void setJavaclass(String javaclass) {
        this.javaclass = javaclass;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
