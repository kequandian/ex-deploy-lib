package com.jfeat.jar.dependency;

import com.jfeat.jar.dependency.comparable.ChecksumKeyValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.jfeat.jar.dependency.FileUtils.JAR_SUFFIX;
import static java.util.function.Predicate.not;

/**
 * 依赖工具类
 *
 * @author zxchengb
 * @date 2020-08-05
 */
public class DependencyUtils {
    /**
     * 依赖匹配正则表达式
     */
    private static final String DEPENDENCY_JAR_NAME_REGEX = "^(\\w+[-.]*\\w*)+\\.jar$";

    /**
     * 检测目标依赖名称是否规范
     * e.g.
     * test.jar(√)
     * ---.jar(×)
     * .jar(×)
     *
     * @param dependencyJarName 待检测的依赖名称
     * @return boolean
     */
    public static boolean isLegal(String dependencyJarName) {
        //System.out.println(dependencyJarName);
        return StringUtils.isNotBlank(dependencyJarName) && dependencyJarName.matches(DEPENDENCY_JAR_NAME_REGEX);
    }

    /**
     * 根据POM文件获取POM原型对象
     *
     * @param pomFile 目标POM文件
     */
    public static Model getPomModel(File pomFile) {
        try (FileInputStream fileInputStream = new FileInputStream(pomFile)) {
            return new MavenXpp3Reader().read(fileInputStream);
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> getDependencies(String path){
        List<String> d;
        if(path.endsWith(JAR_SUFFIX)){
            File jarFile = new File(path);
            d = DependencyUtils.getDependenciesByJar(jarFile);
            d.addAll(DependencyUtils.getDependenciesByPomModel(FileUtils.getPomModelByJar(jarFile)));
        }else{
            d = DependencyUtils.getDependenciesByPom(new File(path.concat(File.separator).concat(FileUtils.POM)));
        }
        return d.stream().distinct().sorted().collect(Collectors.toList());
    }

    /**
     * 通过jar包文件解压，获取其依赖包(lib)并生成依赖集合
     *
     * @param jarFile 目标JAR包
     * @return java.util.List<java.lang.String>
     */
    public static List<String> getDependenciesByJar(File jarFile) {
        List<String> dependencies = new ArrayList<>();
        // 不解压读取压缩包中的文件内容
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile))) {
            ZipEntry zipEntry;
            Set<String> names = new HashSet<>();
            // 循环遍历压缩包内文件对象
            while ((zipEntry = zis.getNextEntry()) != null) {
                names.add(zipEntry.getName());
            }
            names.stream()
                    .filter(s -> s.endsWith(JAR_SUFFIX))
                    .collect(Collectors.toCollection(() -> dependencies));
            //if (dependencies.isEmpty()) {
            //    return getDependenciesByPomModel(FileUtils.getPomModelByJar(jarFile));
            //}
            return dependencies;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dependencies;
    }

    /**
     * 通过jar包文件解压，获取其依赖包(lib)并生成依赖集合
     *
     * @param jarFile 目标JAR包
     * @return java.util.List<java.lang.String>
     */
    public static List<Map.Entry<String,Long>> getChecksumsByJar(File jarFile) {
        List<Map.Entry<String,Long>> dependencies = new ArrayList<>();
        // 不解压读取压缩包中的文件内容
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile))) {
            ZipEntry zipEntry;
            Set<Map.Entry<String,Long>> names = new HashSet<>();

            // 循环遍历压缩包内文件对象
            while ((zipEntry = zis.getNextEntry()) != null) {
                Map.Entry<String,Long> model = Map.entry(zipEntry.getName(), zipEntry.getCrc());
                names.add(model);
            }
            names.stream()
                    .filter(s -> s.getKey().endsWith(JAR_SUFFIX))
                    .collect(Collectors.toCollection(() -> dependencies));
            return dependencies;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return dependencies;
    }

    /**
     * 根据POM文件获取依赖集合
     * Warn：依赖不存在对应值的占位符将原样输出
     * e.g.：test-${no-exist-version}.jar
     *
     * @param pomFile 目标POM文件
     * @return 依赖集合 List
     */
    public static List<String> getDependenciesByPom(File pomFile) {
        return getDependenciesByPomModel(getPomModel(pomFile));
    }

    /**
     * 根据POM实体对象获取依赖集合
     * Warn：依赖不存在对应值的占位符将原样输出
     * e.g.：test-${no-exist-version}.jar
     *
     * @param model 目标POM实体对象
     * @return 依赖集合 List
     */
    public static List<String> getDependenciesByPomModel(Model model) {
        if (model != null) {
            List<String> dependencies = new ArrayList<>();
            final Properties properties = model.getProperties();
            model.getDependencies().forEach(d -> {
                if("test".equals(d.getScope())){
                    // do nothing
                } else {
                    var v = d.getVersion();
                    // 转换version信息
                    v = (v != null ? "-" + (v.startsWith("$") ? properties.getOrDefault(v.substring(v.indexOf("{") + 1, v.lastIndexOf("}")), v) : v) : "");
                    // 生成依赖信息
                    dependencies.add(d.getArtifactId()
                            .concat(v)
                            .concat(".")
                            .concat(d.getType()));
                }
            });
            return dependencies;
        }
        return new ArrayList<>();
    }

    /**
     * 获取不同依赖集合
     *
     * @param origin 源依赖集合
     * @param target 目标依赖集合
     * @return java.util.List<java.lang.String>
     */
    public static List<String> getDifferentDependencies(List<String> origin, List<String> target) {
        return target.stream().filter(not(origin::contains)).sorted().collect(Collectors.toList());
    }

    public static List<Map.Entry<String,Long>> getDifferentChecksums(List<Map.Entry<String,Long>> origin, List<Map.Entry<String,Long>> target) {
        return target.stream()
                .filter(u-> {
                    for(Map.Entry<String,Long> checksum : origin){
                        if(checksum.getKey().equals(u.getKey())) {
                            return ! checksum.getValue().equals(u.getValue());
                        }
                    }
                    return false;
                })
                // convert to ChecksumKeyValue
                .map(u->new ChecksumKeyValue<String,Long>(u.getKey(), u.getValue()))
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<String> getDifferentDependenciesIgnoreVersion(List<String> origin, List<String> target) {
        final String regrex = "(-[0-9\\.]+)([\\-\\.\\w]*).jar";
        final String regrex$ = "(-\\$\\{[\\w\\.\\-]+\\}).jar";

        var queryOrigin = origin.stream()
                .map(u->u.replaceFirst(regrex, JAR_SUFFIX).replaceFirst(regrex$,JAR_SUFFIX))
                .collect(Collectors.toList());
        //queryOrigin.forEach(u->{System.out.println(u.toString());});

        var query = target.stream()
                .map(u->u.replaceFirst(regrex, JAR_SUFFIX).replaceFirst(regrex$,JAR_SUFFIX))
                .filter(not(queryOrigin::contains))
                .sorted()
                .collect(Collectors.toList());
        return query;
    }

    /**
     * 获取相同依赖集合
     *
     * @param origin 源依赖集合
     * @param target 目标依赖集合
     * @return java.util.List<java.lang.String>
     */
    public static List<String> getSameDependencies(List<String> origin, List<String> target) {
        return target.stream()
                .filter(origin::contains)
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<Map.Entry<String,String>> getSameDependenciesIgnoreVersion(List<String> origin, List<String> target) {
        final String regrex = "(-[0-9\\.]+)([\\-\\.\\w]*).jar";
        final String regrex$ = "(-\\$\\{[\\w\\.\\-]+\\}).jar";

        var query = target.stream()
                .map(jar->{
                    String key = jar.replaceFirst(regrex, "").replaceFirst(regrex$,"");
                    for(String it : origin ){
                        if(it.startsWith(key)){
                            return Map.entry(jar, it);
                        }
                    }
                    return Map.entry(jar, "");
                })
                .filter(pair->pair.getValue().length()>0)
                .collect(Collectors.toList());

        return query;
    }

    /**
     * 获取错误（格式错误）依赖集合
     *
     * @param target 目标依赖集合
     * @return java.util.List<java.lang.String>
     */
    public static List<String> getErrorDependencies(List<String> target) {
        return target.stream()
                .filter(not(DependencyUtils::isLegal))
                .sorted()
                .collect(Collectors.toList());
    }
}