package com.jfeat.jar.dependency.cli;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jfeat.jar.dependency.DependencyUtils;
import com.jfeat.jar.dependency.ZipFileUtils;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zxchengb
 * @date 2020-08-05
 *
 */
public class MainMethod {
    /**
     * Dependency list
     */
    private static final String PARSE_OPTION = "p";

    /*
    List all entries from archive
     */
    private static final String LIST_OPTION = "l";
    /*
    Show all entries from archive as tree
     */
    private static final String TREE_OPTION = "t";
    /*
    search entry from archive and show the entry path
     */
    private static final String SEARCH_OPTION = "s";

    /*
    inspect the entry content from archiva, if the entry is jar, list all its entries
     */
    private static final String INSPECT_OPTION = "i";

    /*
    get the groupId of an jar lib
     */
    private static final String GROUPID_OPTION = "g";
    /**
     * 输出为JSON
     */
    private static final String JSON_OPTION = "j";
    /**
     * 同时进行 checksum 比较
     */
    private static final String CHECKSUM_OPTION = "u";


    /**
     * 比较两个JAR
     */
    private static final String COMPARE_OPTION = "c";

    /**
     * 输出相同项，默认输出不同项
     */
    private static final String MATCH_OPTION = "m";
    /**
     *  基于右侧jar输出不相同项（默认为左侧)
     */
    private static final String RIGHT_DIFF_OPTION = "r";

    /**
     * 详细比较
     */
    private static final String VERBOSE_OPTION = "v";


    public static void main(String[] args) {
        Options options = new Options();

        Option dependencyOpt = new Option(PARSE_OPTION, "parse", false, "parse the archive dependencies");
        dependencyOpt.setRequired(false);
        options.addOption(dependencyOpt);

        Option listOpt = new Option(LIST_OPTION, "list", false, "list all the entries from archive");
        listOpt.setRequired(false);
        options.addOption(listOpt);
        Option treeOpt = new Option(TREE_OPTION, "tree", false, "show all the entries from archive as tree");
        treeOpt.setRequired(false);
        options.addOption(treeOpt);
        options.addOption(listOpt);
        Option searchOpt = new Option(SEARCH_OPTION, "search", true, "search criteria within archive");
        searchOpt.setRequired(false);
        options.addOption(searchOpt);

        Option inspectOpt = new Option(INSPECT_OPTION, "inspect", true, "inspect the entry file content");
        inspectOpt.setRequired(false);
        options.addOption(inspectOpt);
        Option groupIdOpt = new Option(GROUPID_OPTION, "groupid", false, "get the jar deployment groupId");
        groupIdOpt.setRequired(false);
        options.addOption(groupIdOpt);

        Option checksumOpt = new Option(CHECKSUM_OPTION, "checksum", false, "with checksum");
        checksumOpt.setRequired(false);
        options.addOption(checksumOpt);
        Option jsonOpt = new Option(JSON_OPTION, "json", false, "output as json format");
        jsonOpt.setRequired(false);
        options.addOption(jsonOpt);

        Option compareOpt = new Option(COMPARE_OPTION, "compare", false, "compare two archive, mismatch from left");
        compareOpt.setRequired(false);
        options.addOption(compareOpt);
        Option rightMisOpt = new Option(RIGHT_DIFF_OPTION, "right", false, "mismatch from right");
        rightMisOpt.setRequired(false);
        options.addOption(rightMisOpt);
        Option matchOpt = new Option(MATCH_OPTION, "match", false, "compare matches");
        matchOpt.setRequired(false);
        options.addOption(matchOpt);

        // verbose compare
        Option verboseOpt = new Option(VERBOSE_OPTION, "verbose", false, "compare mismatch with verbose");
        verboseOpt.setRequired(false);
        options.addOption(verboseOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;//not a good practice, it serves it purpose
        File jar1=null, jar2=null;
        try {
            cmd = parser.parse(options, args);
            if (cmd.getArgList().size() == 0) {
                throw new ParseException("no arg!");
            }
            String jar1arg = cmd.getArgs()[0];
            jar1 = new File(jar1arg);
            if(!jar1.exists()){
                throw new ParseException(" not exits !");
            }

            if (cmd.hasOption(COMPARE_OPTION)) {
                if (cmd.getArgList().size() < 2) {
                    throw new ParseException("require 2 jars to compare !");
                }
                String jar2arg = cmd.getArgs()[1];
                jar2 = new File(jar2arg);
                if(!jar2.exists()){
                    throw new ParseException(jar2arg + " not exits !");
                }
            }

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("jar-dependency [OPTIONS] <jars...>", options);

            System.exit(1);
        }

        // 获取依赖
        if (cmd.hasOption(PARSE_OPTION)) {
            List<String> d1 = cmd.hasOption(CHECKSUM_OPTION) ? getChecksumDependencies(jar1)
                    : DependencyUtils.getDependenciesByJar(jar1);
            printOut(d1, cmd.hasOption(JSON_OPTION));

        }else if (cmd.hasOption(LIST_OPTION)) {
            try {
                List<String> d1 = ZipFileUtils.listEntriesFromArchive(jar1, "", "");
                printOut(d1, cmd.hasOption(JSON_OPTION));
            }catch (IOException e){
            }
        }else if(cmd.hasOption(TREE_OPTION)){
            List<String> d1 = ZipFileUtils.getJarArchiveTree(jar1, cmd.hasOption(CHECKSUM_OPTION));
            printOut(d1, cmd.hasOption(JSON_OPTION));
        }else if(cmd.hasOption(SEARCH_OPTION)){
            List<String> d1 = ZipFileUtils.searchWithinJarArchive(jar1, cmd.getOptionValue(SEARCH_OPTION), cmd.hasOption(CHECKSUM_OPTION));


            // start group
                String currentKey = null;
                Map<String,List<String>> map=new HashMap();
                for(int i=0;i<d1.size();i++){
                    String key=d1.get(i);
                    // 判断是否以+- 开头
                    boolean isValue = key.startsWith("+- ");
                    if(!isValue){
                        // 保存key值
                        currentKey = key;
                        // key
                        if(map.containsKey(key)){
                        }else{
                            map.put(key, null);
                        }
                    }else{
                        List<String> list = map.get(currentKey);
                        if(list==null){
                            list = new ArrayList<>();
                            map.put(currentKey, list);
                        }
                        list.add(key);
                    }
                }

                List<String> d2 = new ArrayList<>();
                for (String key: map.keySet()) {
                    d2.add(key);
                    List<String> list = map.get(key);
                    if(list!=null) {
                        for (int i = 0; i < list.size(); i++) {
                            d2.add(list.get(i));
                        }
                    }
                }
            /// end group


            printOut(d2, cmd.hasOption(JSON_OPTION));
        }
        else if(cmd.hasOption(INSPECT_OPTION)){
            String entryPattern  = cmd.getOptionValue(INSPECT_OPTION);
            final String NewLine = "\n";
            var entriesContent = ZipFileUtils.getJarEntryPatternContent(jar1, entryPattern, true);
            if(entriesContent.contains(NewLine)){
                // means multi matches, just output all entries
                if(StringUtils.isNotBlank(entriesContent)) {
                    var entries = Stream.of(entriesContent.split(NewLine)).collect(Collectors.toList());
                    printOut(entries, cmd.hasOption(JSON_OPTION));
                }
            }else{
                if(FileUtils.extension(entriesContent).equals("jar")){
                    // bin, print out the entries
                    var entries = ZipFileUtils.getJarEntriesWithinEntry(jar1, entriesContent);
                    printOut(entries, cmd.hasOption(JSON_OPTION));

                }else{
                    // print out the entry content
                    entriesContent = ZipFileUtils.getJarEntryPatternContent(jar1, entryPattern, false);
                    if(StringUtils.isNotBlank(entriesContent)) {
                        var entries = Stream.of(entriesContent.split(NewLine)).collect(Collectors.toList());
                        printOut(entries, cmd.hasOption(JSON_OPTION));
                    }
                }
            }

        }else if(cmd.hasOption(GROUPID_OPTION)){
            System.out.println(ZipFileUtils.getJarGroupId(jar1));

        }else {
            List<String> result = new ArrayList<>();

            if (cmd.hasOption(COMPARE_OPTION)) {
                List<String> d1 = cmd.hasOption(CHECKSUM_OPTION)? getChecksumDependencies(jar1) : DependencyUtils.getDependenciesByJar(jar1);
                List<String> d2 = cmd.hasOption(CHECKSUM_OPTION)? getChecksumDependencies(jar2) : DependencyUtils.getDependenciesByJar(jar2);

                if (d1.isEmpty()) { d1.add(cmd.hasOption(CHECKSUM_OPTION)? jarToEntryWithChecksum(jar1.getAbsolutePath()) : jarToEntry(jar1.getAbsolutePath())); }
                if (d2.isEmpty()) { d2.add(cmd.hasOption(CHECKSUM_OPTION)? jarToEntryWithChecksum(jar2.getAbsolutePath()) : jarToEntry(jar1.getAbsolutePath())); }

                if(cmd.hasOption(MATCH_OPTION)) {
                    result.addAll(DependencyUtils.getSameDependencies(d1, d2));

                }else if(cmd.hasOption(RIGHT_DIFF_OPTION)){
                    // mismatch from right
                    if(cmd.hasOption(VERBOSE_OPTION)) {
                        result.addAll(getDependenciesMismatchVerbose(d2, d1, FileUtils.filename(jar2.getName()), FileUtils.filename(jar1.getName())));
                    }else{
                        result.addAll(DependencyUtils.getDifferentDependencies(d2, d1));
                    }

                }else{
                    // mismatch from left as default
                    if(cmd.hasOption(VERBOSE_OPTION)){
                        result.addAll(getDependenciesMismatchVerbose(d1, d2, FileUtils.filename(jar1.getName()), FileUtils.filename(jar2.getName())));
                    }else {
                        result.addAll(DependencyUtils.getDifferentDependencies(d1, d2));
                    }

                }
            }
            printOut(result, cmd.hasOption(JSON_OPTION));
        }
    }


    /*
      不同依赖项的详细对比
     */
    private static List<String> getDependenciesMismatchVerbose(List<String> d1, List<String> d2, String jar1, String jar2) {
        List<String> diff = DependencyUtils.getDifferentDependencies(d1, d2);

        var verbose = diff.stream()
                .map(u->{
                    String entryWithoutChecksum = u.contains("@") ? u.substring(0,u.indexOf("@")):u;
                    String entryWithoutVersion  = entryWithoutChecksum.replaceAll("\\-[0-9\\.]+\\-?[a-zA-Z]*.jar", "");

                    // get the diff d1
                    String diffOne = d1.stream().filter(e->e.startsWith(entryWithoutVersion)).collect(Collectors.joining());
                    if(StringUtils.isNotBlank(diffOne)){
                        return String.join("", jar1, "!", diffOne, "\t", u);
                    }
                    return String.join("", jar1, "\t\t", u);
                })
                .collect(Collectors.toList());

        return verbose;
    }

    /*
     获取文件的依赖以及 checksum
     */
    private static List<String> getChecksumDependencies(File jarFile) {
        List<String> d1 = new ArrayList<>();
        try {
            var checksums = ZipFileUtils.listEntriesWithChecksum(jarFile, "jar", "");
            checksums.stream().forEach(
                    p -> d1.add(java.lang.String.join("@", p.getKey(), java.lang.String.valueOf(p.getValue())))
            );
        } catch (Exception e) {
        }
        return d1;
    }


    /*
     打印依赖项
     */
    private static void printOut(List<String> result, boolean jsonFormat){
        if (jsonFormat) {
            System.out.println(
                    JSONArray.toJSONString(result, SerializerFeature.PrettyFormat)
            );
        } else {
            result.stream().forEach(
                    p->System.out.println(p)
            );
        }
    }
    /*
      由单个文件名转换为 BOOT-INF/lib/<>
     */
    private static String jarToEntry(String jarPath){
        if(FileUtils.extension(jarPath).equals("jar")){
            return String.join("", "BOOT-INF/lib/", FileUtils.filename(jarPath));
        }else if(FileUtils.extension(jarPath).equals("war")){
            return String.join("", "WEB-INF/lib/", FileUtils.filename(jarPath));
        }
        return FileUtils.basename(jarPath);
    }
    /*
      由单个文件名转换为 BOOT-INF/lib/<>, 并获取文件checksum
     */
    private static String jarToEntryWithChecksum(String jarPath){
        String entry = jarToEntry(jarPath);
        try {
            return String.join("@", entry, String.valueOf(ZipFileUtils.getFileChecksumCode(new File(jarPath), "adler32").padToLong()));
        }catch (IOException e){
        }
        return entry;
    }
}