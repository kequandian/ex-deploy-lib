package com.jfeat.jar.dependency.cli;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
     * mismatch only 输出标识
     */
    private static final String MISMATCH_FLAG = "m";

    /**
     * 对比标识
     */
    private static final String COMPARE_REGEX = "^-[c][mj]*?$";
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
    private static final String OPTION_REGEX = "^-[c][mj]*?$|^-[p](j)?$|^-[v]$";

    public static void Help(){
        System.out.println(
        "Usage: dependency [Options] [.jar ...]\n" +
        "  e.g. dependency -p ./lib/test.jar\n" +
        "\n" +
        "Options:\n" +
        "-c, --compare </path/to/module1> </path/to/module2>  Compare two jar module\n" +
        "-m, --mismatch </path/to/module1> </path/to/module2>  Compare two jar module, only mismatch ones\n" +
        "-j, --json     print out by json format\n" +
        "-p, --parse </path/to/the-app.jar> [...]  parse jar dependencies and print out\n" +
        "-v, --version  print out version info"
        );
    }

    public static void main(String[] args) {
        //  -d  --download  <groupId:artifactId:Version>
        if (args.length == 0) {
            Help();
            return;
        }

        String option = args[0];
        if (option != null && !option.isBlank() && option.matches(OPTION_REGEX)) {

            if (option.matches(COMPARE_REGEX)) {
                if(args.length >= 3) {
                    String module1 = args[1];
                    String module2=  args[2];
                    compare(module1, module2, option);
                }else{
                    Help();
                }
            } else if (option.matches(PARSE_REGEX) && args.length >= 2) {
                if (option.contains(JSON_FLAG)) {
                    Arrays.stream(args).skip(1).forEach(s -> {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(s.substring(s.lastIndexOf('/')+1), parse(s));
                        System.out.println(JSONObject.toJSONString(jsonObject, SerializerFeature.PrettyFormat));
                    });
                } else {
                    Arrays.stream(args).skip(1).forEach(
                            s -> parse(s)
                                    .stream()
                                    .takeWhile(not(String::isBlank))
                                    .forEach(System.out::println));
                }
            } else if (option.matches(VERSION_REGEX)) {
                Properties properties = FileUtils.getProperties();
                if (properties != null) {
                    System.err.println(properties.getProperty("app.version"));
                }
            }

        }else{
            Help();
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
                System.err.println("NOT Found Dependency JAR file.");
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
                final List<String> leftDifferentDependencies = DependencyUtils.getDifferentDependencies(d1, d2);

                if (option.contains(JSON_FLAG)) {

                    if(option.contains(MISMATCH_FLAG)){
                        JSONArray array = new JSONArray();
                        System.out.println(JSON.toJSONString(array, SerializerFeature.PrettyFormat));
                    }else {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("matches", sameDependencies);
                        jsonObject.put("mismatches", leftDifferentDependencies);
                        //jsonObject.put(rightName, rightDifferentDependencies);
                        System.out.println(JSON.toJSONString(jsonObject, SerializerFeature.PrettyFormat));
                    }

                } else {
                    if(option.contains(MISMATCH_FLAG)) {
                        if (!leftDifferentDependencies.isEmpty()) {
                            leftDifferentDependencies.forEach(s -> System.out.println("\t" + s));
                        }
                    }else{
                        if (!sameDependencies.isEmpty()) {
                            System.out.println("matches");
                            sameDependencies.forEach(s -> System.out.println("\t\t\t" + s));
                        }
                        if (!leftDifferentDependencies.isEmpty()) {
                            System.out.println("mismatches");
                            leftDifferentDependencies.forEach(s -> System.out.println("\t\t\t" + s));
                        }
                    }
                }

            } else {
                System.err.println("ERROR.");
            }
        }
    }
}