package com.jfeat.jar.dependency;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author zxchengb
 * @date 2020-08-05
 */
public class FileUtils {
    /**
     * 依赖JAR包前缀锚点
     */
    //@Deprecated
    //public static final String LIB_JAR_DIR = "BOOT-INF/lib/";

    /**
     * JAR包通用后缀
     */
    public static final String JAR_SUFFIX = ".jar";
    /**
     * JAR包依赖输出文件后缀
     */
    public static final String DEPENDENCIES_OUT_PUT_JSON_FILE_PREFIX = "_dependencies_";
    /**
     * JAR包依赖输出文件后缀
     */
    public static final String DEPENDENCIES_OUT_PUT_JSON_FILE_SUFFIX = ".json";
    /**
     * POM文件标识
     */
    public static final String POM = "pom.xml";

    /**
     * 将内容写入目标文件中
     *
     * @param file    目标文件
     * @param content 写入内容
     * @return boolean
     */
    public static boolean writeContext(File file, String... content) {
        if (file != null && content != null) {
            try (BufferedWriter write = new BufferedWriter(new FileWriter(file))) {
                for (String s : content) {
                    write.write(s);
                    write.newLine();
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 读取目标文件内容并返回
     *
     * @param file 目标文件
     * @return java.com.jfeat.util.List<java.lang.String>
     */
    public static List<String> readContext(File file) {
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                List<String> list = new ArrayList<>();
                String temp;
                while ((temp = reader.readLine()) != null) {
                    list.add(temp);
                }
                return list;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 格式化JSON数据形式写入目标文件
     *
     * @param file   目标文件
     * @param object 待格式化的JSON对象
     */
    public static boolean writeContextInJSON(File file, JSONObject object) {
        return FileUtils.writeContext(file, JSON.toJSONString(object, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat));
    }

    /**
     * 获取资源文件属性
     *
     * @return Properties
     */
    public static Properties getProperties() {
        URL resource = Thread.currentThread().getContextClassLoader().getResource("resources.properties");
        if (resource != null) {
            try (InputStream in = resource.openStream()) {
                Properties props = new Properties();
                props.load(in);
                in.close();
                return props;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 根据JAR包获取POM实体模型对象
     *
     * @param jarFile JAR包对象
     * @return POM实体模型对象
     */
    public static Model getPomModelByJar(File jarFile) {
        // 不解压读取JAR包中的pom实体对象
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile))){
            ZipEntry zipEntry;
            // 循环遍历压缩包内文件对象
            while ((zipEntry = zis.getNextEntry()) != null) {
                if(zipEntry.getName().contains(POM)){
                    return new MavenXpp3Reader().read(new ZipFile(jarFile).getInputStream(zipEntry));
                }
            }
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取文件的相对路径
     * @param referenceFile 参照文件
     * @param file  目标文件相对路径
     * @return
     */
    public static String getRelativeFilePath(File referenceFile, File file) {
        String firstFilePath = referenceFile.getAbsolutePath();
        String filePath = file.getAbsolutePath();

        String commonPath = org.codehaus.plexus.util.FileUtils.dirname(filePath);
        while (!firstFilePath.startsWith(commonPath)){
            commonPath = org.codehaus.plexus.util.FileUtils.dirname(commonPath);
        }
        return filePath.replace(commonPath, "");

//        String entryPath = FileUtils.dirname(file.getAbsolutePath())
//                .substring(ileUtils.dirname(referenceFile.getAbsolutePath()).length() + 1);
//
//        return String.join(File.separator, entryPath, org.codehaus.plexus.util.FileUtils.filename(file.getAbsolutePath()));
    }

    public static String getRelativePath(String referencePath, String filePath) {
        String commonPath = org.codehaus.plexus.util.FileUtils.dirname(filePath);
        while (!referencePath.startsWith(commonPath)){
            commonPath = org.codehaus.plexus.util.FileUtils.dirname(commonPath);
        }
        return filePath.replace(commonPath, "");
    }

    public static String getCommonBasedir(String firstPath, String secondPath){
        String commonPath = org.codehaus.plexus.util.FileUtils.dirname(secondPath);
        while (!firstPath.startsWith(commonPath)){
            commonPath = org.codehaus.plexus.util.FileUtils.dirname(commonPath);
        }
        return commonPath;
    }
}