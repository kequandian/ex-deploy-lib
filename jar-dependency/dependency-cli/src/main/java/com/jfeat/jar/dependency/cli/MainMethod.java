package com.jfeat.jar.dependency.cli;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jfeat.jar.dependency.DependencyUtils;
import com.jfeat.jar.dependency.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.util.function.Predicate.not;

/**
 * @author zxchengb
 * @date 2020-08-05
 */
public class MainMethod {
    /**
     * JSON输出标识
     */
    private static final String JSON_FLAG = "j";
    /**
     * Boolean 输出标识
     */
    private static final String BOOLEAN_FLAG = "b";
    /**
     * 对比标识
     */
    private static final String COMPARE_REGEX = "^-[c][jb]?$";
    /**
     * 解析标识
     */
    private static final String PARSE_REGEX = "^-[p][j]?$";
    /**
     * 版本标识
     */
    private static final String VERSION_REGEX = "^-v$";
    /**
     * 选项正则表达式
     */
    private static final String OPTION_REGEX = "^-[c][jb]?$|^-[p](j)?$|^-[v]$";

    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }
        String option = args[0];
        if (option != null && !option.isBlank() && option.matches(OPTION_REGEX)) {
            if (option.matches(COMPARE_REGEX) && args.length >= 3) {
                compare(args[1], args[2], option);
            } else if (option.matches(PARSE_REGEX) && args.length >= 2) {
                if (option.contains(JSON_FLAG)) {
                    Arrays.stream(args).skip(1).forEach(s -> {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("dependencies", parse(s));
                        System.out.println(JSONObject.toJSONString(jsonObject, SerializerFeature.PrettyFormat));
                    });
                } else {
                    Arrays.stream(args).skip(1).forEach(s -> parse(s).stream().takeWhile(not(String::isBlank)).forEach(System.out::println));
                }
            } else if (option.matches(VERSION_REGEX)) {
                Properties properties = FileUtils.getProperties();
                if (properties != null) {
                    System.err.println(properties.getProperty("app.version"));
                }
            }
        }
    }

    /**
     * 根据JAR包路径解析并生成依赖结果
     *
     * @param filePath 目标JAR包路径
     */
    private static List<String> parse(String filePath) {
        File jarFile = new File(filePath);
        if (jarFile.exists() && jarFile.isFile()) {
            List<String> dependencies = DependencyUtils.getDependencies(filePath);
            if (dependencies != null && !dependencies.isEmpty()) {
                return dependencies;
            } else {
                System.out.println("NOT Found Dependency JAR file.");
            }
        } else {
            System.err.println("NOT Found JAR File: " + filePath);
        }
        return new ArrayList<>();
    }

    /**
     * 根据两个Maven module查找对应的依赖并将对比结果输出为JSON文件
     *
     * @param module_1 目标模块1
     * @param module_2 目标模块2
     */
    private static void compare(String module_1, String module_2, String option) {
        if (option.matches(COMPARE_REGEX)) {
            List<String> d1 = DependencyUtils.getDependencies(module_1);
            List<String> d2 = DependencyUtils.getDependencies(module_2);
            if (!d1.isEmpty() && !d2.isEmpty()) {
                final List<String> sameDependencies = DependencyUtils.getSameDependencies(d1, d2);
                final List<String> leftDifferentDependencies = DependencyUtils.getDifferentDependencies(d2, d1);
                final List<String> rightDifferentDependencies = DependencyUtils.getDifferentDependencies(d1, d2);
                String left = new File(module_1).getName();
                String right = new File(module_2).getName();
                var leftName = left + "-mismatches";
                var rightName = right + "-mismatches";
                if(option.contains(BOOLEAN_FLAG)){
                    boolean result = rightDifferentDependencies.isEmpty();
                    System.out.println(result);
                    System.out.println(result ? right +" CAN be injected into "+ left +"." : right +" CAN'T be injected into "+ left +".");
                }
                else if (option.contains(JSON_FLAG)) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("matches", sameDependencies);
                    jsonObject.put(leftName, leftDifferentDependencies);
                    jsonObject.put(rightName, rightDifferentDependencies);
                    System.out.println(JSON.toJSONString(jsonObject, SerializerFeature.PrettyFormat));
                } else {
                    if (!sameDependencies.isEmpty()) {
                        System.out.println("matches");
                        sameDependencies.forEach(s -> System.out.println("\t\t\t" + s));
                    }
                    if (!leftDifferentDependencies.isEmpty()) {
                        System.out.println(leftName);
                        leftDifferentDependencies.forEach(s -> System.out.println("\t\t\t" + s));
                    }
                    if (!rightDifferentDependencies.isEmpty()) {
                        System.out.println(rightName);
                        rightDifferentDependencies.forEach(s -> System.out.println("\t\t\t" + s));
                    }
                }
            } else {
                System.err.println("ERROR.");
            }
        }
    }
}