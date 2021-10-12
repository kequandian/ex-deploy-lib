package com.jfeat.am.jar.dep.util;

import com.jfeat.jar.dependency.ZipFileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IndexingUtils {
    public static List<String> indexingJarFile(File jarFile, String entryExtension, String entryPattern, File indexesPath, boolean recreate) throws IOException{
        if(!indexesPath.exists()) {
            org.codehaus.plexus.util.FileUtils.mkdir(indexesPath.getAbsolutePath());
        }

        List<String> indexes = ZipFileUtils.listEntriesFromArchive(jarFile, entryExtension, entryPattern);

        // clean up all indexing files first
        if(recreate){
            indexes.stream().forEach(entry -> {
                String firstLetter = String.valueOf(org.codehaus.plexus.util.FileUtils.filename(entry.replace("/",File.separator)).charAt(0)).toLowerCase();
                File letterFile = new File(String.join(File.separator, indexesPath.getAbsolutePath(), firstLetter));
                if(letterFile.exists()) {
                    try {
                        org.codehaus.plexus.util.FileUtils.forceDelete(letterFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // 创建索引
        // create indexes files
        String jarFilename = jarFile.getName();
        indexes.stream()
                //.filter(f->org.codehaus.plexus.util.FileUtils.extension(f).equals("class"))
                .map(key->{
                    Map.Entry<String,String> entry = Map.entry(key, jarFilename);
                    return entry;
                }).forEach(entry->{
            String fileName = org.codehaus.plexus.util.FileUtils.filename(entry.getKey().replace("/", File.separator));
            String firstLetter = String.valueOf(fileName.charAt(0)).toLowerCase();
            File letterFile = new File(String.join(File.separator, indexesPath.getAbsolutePath(), firstLetter));

            try {
                // skip exist ones
                // read content from file
                List<String> lines = letterFile.exists() ? org.apache.commons.io.FileUtils.readLines(letterFile, "UTF-8") : new ArrayList<>();
                List<String> contents = lines.stream().map(line->{
                    return line.split(",")[0];
                }).collect(Collectors.toList());

                // append to file
                if(!contents.contains(entry.getKey())) {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(letterFile, true));
                    bw.append(String.join(",", fileName, entry.getValue(), entry.getKey(), "\n"));
                    bw.close();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });

        return indexes;
    }
}
