package com.jfeat.jar.dependency;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.*;

public class ZipFileUtils {
    /*
        get checksum of file or entries in fatjar
     */
    private static String CHECKSUM_OPT = "s";
    /*
    List the table of contents for the archive
     */
    private static String LIST_OPT = "t";

    /*
     filter extension and pattern for entries
     */
    private static String EXTENSION_OPT = "e";
    private static String PATTERN_OPT = "a";

    /*
     inspect file content
     */
    private static String INSPECT_ENTRY_OPT = "i";
    /*
    inspect Manifest
     */
    private static String INSPECT_MANIFEST_OPT = "m";
    /*
    inspect pom.xml
     */
    private static String INSPECT_POM_OPT = "o";
    /*
    inspect groupId
     */
    private static String INSPECT_GROUPID_OPT = "g";



    public static void main(String[] args) throws IOException {
        /**
         * e.g.
         * java -cp target/jar-dependency.jar com.jfeat.jar.dependency.ZipFileUtils target/jar-dependency.jar -s -p ZipFileUtils -e class
         */
        Options options = new Options();

        Option listOpt = new Option(LIST_OPT, "list", false, "List the table of contents for the archive");
        listOpt.setRequired(false);
        options.addOption(listOpt);

        Option checksumOpt = new Option(CHECKSUM_OPT, "checksum", false, "get file checksum");
        checksumOpt.setRequired(false);
        options.addOption(checksumOpt);

        Option filterExtOpt = new Option(EXTENSION_OPT, "extension", true, "filter of entry extensions");
        filterExtOpt.setRequired(false);
        options.addOption(filterExtOpt);
        Option filterPatternOpt = new Option(PATTERN_OPT, "pattern", true, "filter of entry pattern");
        filterPatternOpt.setRequired(false);
        options.addOption(filterPatternOpt);

        Option inspectOpt = new Option(INSPECT_ENTRY_OPT, "inspect", true, "view file content");
        inspectOpt.setRequired(false);
        options.addOption(inspectOpt);

        Option inspectManifestOpt = new Option(INSPECT_MANIFEST_OPT, "manifest", false, "view MANIFEST.MF content");
        inspectManifestOpt.setRequired(false);
        options.addOption(inspectManifestOpt);
        Option inspectPomOpt = new Option(INSPECT_POM_OPT, "pom", false, "view dependency pom.xml content");
        inspectPomOpt.setRequired(false);
        options.addOption(inspectPomOpt);
        Option inspectGroupIdOpt = new Option(INSPECT_GROUPID_OPT, "groupId", false, "get dependency groupId");
        inspectGroupIdOpt.setRequired(false);
        options.addOption(inspectGroupIdOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;//not a good practice, it serves it purpose
        File jarFile = null;
        String jarEntry = null;

        try {
            cmd = parser.parse(options, args);
            if(cmd.getArgList().size()==0){
                throw new ParseException("no arg!");
            }
            jarEntry = cmd.getArgs()[0];
            jarFile = new File(jarEntry);
            if (!jarFile.exists()) {
                throw new ParseException(jarFile.getName() + " not exist !");
            }

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("jar-dependency [OPTIONS] <checksum-file>", options);

            System.exit(1);
        }

        if(cmd.hasOption(LIST_OPT)){
            String extension =  cmd.hasOption(EXTENSION_OPT) ? cmd.getOptionValue(EXTENSION_OPT) : "";
            String pattern =  cmd.hasOption(PATTERN_OPT) ? cmd.getOptionValue(PATTERN_OPT) : "";
            listEntriesFromArchive(jarFile, extension, pattern).stream()
                    .forEach(
                    p->System.out.println(p)
            );
        }
        else if(cmd.hasOption(CHECKSUM_OPT) && (cmd.hasOption(EXTENSION_OPT)||cmd.hasOption(PATTERN_OPT))){
            // means fatjar, get entries
            String extension =  cmd.hasOption(EXTENSION_OPT) ? cmd.getOptionValue(EXTENSION_OPT) : "";
            String pattern =  cmd.hasOption(PATTERN_OPT) ? cmd.getOptionValue(PATTERN_OPT) : "";

            var checksums = listEntriesWithChecksum(jarFile, extension, pattern);
            checksums.stream().forEach(
                    p->System.out.println(String.join("@", p.getKey(),String.valueOf(p.getValue())))
            );
        }else if(cmd.hasOption(CHECKSUM_OPT)) {
            // get file checksum
            String filePath = cmd.getArgs()[0];
            File libFile = new File(filePath);
            var checksum = getFileChecksumCode(libFile, "adler32");
            System.out.println(checksum.padToLong());

        }else if(cmd.hasOption(INSPECT_ENTRY_OPT)){
            System.out.println(getJarEntryPatternContent(jarFile, cmd.getOptionValue(INSPECT_ENTRY_OPT), false));
        }else if(cmd.hasOption(INSPECT_MANIFEST_OPT)){
            System.out.println(getJarManifestContent(jarFile));
        }else if(cmd.hasOption(INSPECT_POM_OPT)){
            System.out.println(getJarPomContent(jarFile));
        }else if(cmd.hasOption(INSPECT_GROUPID_OPT)){
            System.out.println(getJarGroupId(jarFile));
        }
    }

    /**
     *  大JAR文件中列出压缩文件
     * @param zipFile
     * @param entryExtension 文件类型过滤
     * @param entryPattern  文件名匹配过滤 （包含逻辑）
     * @return
     * @throws IOException
     */
    public static List<String> listEntriesFromArchive(File zipFile, String entryExtension, String entryPattern) throws IOException{
        try (
                InputStream zipStream = new FileInputStream(zipFile);
                ZipInputStream zis =
                        new ZipInputStream(new BufferedInputStream(zipStream))) {

            // within try
            ZipEntry entry = null;
            List<String> entries = new ArrayList<>();

            while ((entry = zis.getNextEntry()) != null) {
                if ( (StringUtils.isBlank(entryExtension) || entryExtension.equals(FileUtils.extension(entry.getName()))) &&
                        (StringUtils.isBlank(entryPattern) || entry.getName().contains(entryPattern))
                ){
                    entries.add(entry.getName());
                }
            }

            return entries;
        }
    }

    /**
     * 在zipFile中获取匹配pattern文件的checksum信息并排序
     * @param zipFile
     * @param entryExtension 文件后缀过滤
     * @param entryPattern 符合条件的搜索 （是否包含内容）
     * @return
     * @throws IOException
     */
    public static List<Map.Entry<String,Long>> listEntriesWithChecksum(File zipFile, String entryExtension, String entryPattern) throws IOException {
        try (
                InputStream zipStream = new FileInputStream(zipFile);
                // Creating input stream that also maintains the checksum of
                // the data which later can be used to validate data
                // integrity.
                CheckedInputStream cs =
                        new CheckedInputStream(zipStream, new Adler32());
                ZipInputStream zis =
                        new ZipInputStream(new BufferedInputStream(cs))) {

            // within try
            ZipEntry entry = null;
            List<Map.Entry<String,Long>> checksums = new ArrayList<>();

            // Read each entry from the ZipInputStream until no more entry
            // found indicated by a null return value of the getNextEntry()
            // method.
            while ((entry = zis.getNextEntry()) != null) {
                if ( (StringUtils.isBlank(entryExtension) || entryExtension.equals(FileUtils.extension(entry.getName()))) &&
                        (StringUtils.isBlank(entryPattern) || entry.getName().contains(entryPattern))
                ) {
                    checksums.add(Map.entry(entry.getName(), entry.getCrc()));
                }
            }

            // Print out the checksum value
            return checksums;
        }
    }

    /**
     * extra jar entry within jar
     */
    public static List<Map.Entry<String,Long>> extraJarEntriesWithChecksum(File file, String extension, String pattern, String destDir) {
        List<Map.Entry<String,Long>> entries = new ArrayList<>();

        try(JarFile jarFile = new JarFile(file);
            CheckedInputStream cs =
                    new CheckedInputStream(new FileInputStream(file), new Adler32());
            JarInputStream jarInputStream =
                    new JarInputStream(new BufferedInputStream(cs));
            ) {

//            Enumeration enumEntries = jarInputStream.entries();
            JarEntry jarEntry = null;
            while ((jarEntry = jarInputStream.getNextJarEntry())!=null) {
                if((StringUtils.isBlank(extension) || FileUtils.extension(jarEntry.getName()).equals(extension)) &&
                        StringUtils.isBlank(pattern) || jarEntry.getName().contains(pattern)) {

                    File f = new File(String.join(File.separator, destDir, jarEntry.getName()));
                    if (jarEntry.isDirectory()) { // if its a directory, create it
                        f.mkdir();
                        continue;
                    }
                    InputStream is = jarFile.getInputStream(jarEntry); // get the input stream
                    FileOutputStream fos = new FileOutputStream(f);
                    while (is.available() > 0) {  // write contents of 'is' to 'fos'
                        fos.write(is.read());
                    }
                    fos.close();
                    is.close();

                    entries.add(Map.entry(jarEntry.getName(), jarEntry.getCrc()));
                }
            }
        }catch (IOException e){
        }
        return entries;
    }

    /**
     * extra jar entry within jar
     */
    public static List<String> extraJarEntries(File jarFile, String extension, String pattern, String destDir) {
        List<String> entries = new ArrayList<>();
        try(JarFile jar = new JarFile(jarFile)) {
            Enumeration enumEntries = jar.entries();
            while (enumEntries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumEntries.nextElement();
                if((StringUtils.isBlank(extension) || FileUtils.extension(jarEntry.getName()).equals(extension)) &&
                        StringUtils.isBlank(pattern) || jarEntry.getName().contains(pattern)) {

                    java.io.File f = new java.io.File(String.join(File.separator, destDir, jarEntry.getName()));
                    if (jarEntry.isDirectory()) { // if its a directory, create it
                        f.mkdir();
                        continue;
                    }
                    java.io.InputStream is = jar.getInputStream(jarEntry); // get the input stream
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                    while (is.available() > 0) {  // write contents of 'is' to 'fos'
                        fos.write(is.read());
                    }
                    fos.close();
                    is.close();

                    entries.add(jarEntry.getName());
                }
            }
        }catch (IOException e){
        }
        return entries;
    }

    /**
     * get single file checksum
     * @param file
     * @param hashCode
     * @return
     * @throws IOException
     */
    public static HashCode getFileChecksumCode(File file, String hashCode) throws IOException{
        HashCode checksumCode =  HashCode.fromInt(0);
        if(StringUtils.isNotEmpty(hashCode)) {
            final String[] supportedType  =new String[]{"adler32","crc32","crc32c","md5","sha1","sha256","sha512"};
            //Assertions.assertThat(Stream.of(supportedType).collect(Collectors.toList()).contains(hashCode));

            switch (hashCode) {
                case "adler32":
                    checksumCode = Files.hash(file, Hashing.adler32());
                    break;
                case "crc32":
                    checksumCode = Files.hash(file, Hashing.crc32());
                    break;
                case "crc32c":
                    checksumCode = Files.hash(file, Hashing.crc32c());
                    break;
                case "md5":
                    checksumCode = Files.hash(file, Hashing.md5());
                    break;
                case "sha1":
                    checksumCode = Files.hash(file, Hashing.sha1());
                    break;
                case "sha256":
                    checksumCode = Files.hash(file, Hashing.sha256());
                    break;
                case "sha512":
                    checksumCode = Files.hash(file, Hashing.sha512());
                    break;
                default:
                    break;
            }
        }
        return checksumCode;
    }
    public static String getFileChecksum(File file, String hashCode) throws IOException{
        return getFileChecksumCode(file, hashCode).toString();
    }

    /*
       get entry content
     */
    public static String getJarEntryPatternContent(File jarFile, String entryNamePattern, boolean forcePath){
        StringBuilder content = new StringBuilder();
        try(JarFile jar = new JarFile(jarFile)) {
            Enumeration enumEntries = jar.entries();
            List<String> entryEffected = new ArrayList<>();
            JarEntry matchedJarEntry = null;
            boolean ret=true;
            while (ret && enumEntries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumEntries.nextElement();
                if (jarEntry.getName().contains(entryNamePattern)) {
                    entryEffected.add(jarEntry.getName());
                    matchedJarEntry = jarEntry;
                }
                if(jarEntry.getName().equals(entryNamePattern)){
                    ret = false;
                }
            }

            if(entryEffected.size()==1) {
                if(forcePath){
                    // force to path
                    content.append(entryEffected.get(0));
                }else {
                    java.io.InputStream is = jar.getInputStream(matchedJarEntry); // get the input stream
                    BufferedReader r = new BufferedReader(
                            new InputStreamReader(is));
                    String line = null;
                    String NewLine = "\n";
                    while ((line = r.readLine()) != null) {
                        line = r.readLine();
                        if (line != null) {
                            content.append(line);
                            content.append(NewLine);
                        }
                    }
                    is.close();
                }
            }else{
                entryEffected.forEach(e->content.append(e + "\n"));
            }
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
        return content.toString();
    }

    public static String getZipEntryContent(File file, String entryName) {
        StringBuilder content = new StringBuilder();
        try(JarFile jarFile = new JarFile(file)) {
            Manifest manifest = jarFile.getManifest();

            ZipEntry zipEntry = jarFile.getEntry(entryName);
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(jarFile.getInputStream(zipEntry)));
            String line = r.readLine();
            String NewLine = "\n";
            while (line != null) {
                line = r.readLine();
                if(line!=null) {
                    content.append(line);
                    content.append(NewLine);
                }
            }
        }catch (Exception e){
        }
        return content.toString();
    }

    public static String getJarManifestContent(File jarFile) {
        return getZipEntryContent(jarFile, "META-INF/MANIFEST.MF");
    }

    /*
    列出archiva中的jar类型entry中的文件
     */
    public static List<String> getJarEntriesWithinEntry(File file, String entryName){
        List<String> entries = new ArrayList<>();

        try(JarFile jarFile = new JarFile(file)) {
            ZipEntry zipEntry = jarFile.getEntry(entryName);
            try(InputStream is = jarFile.getInputStream(zipEntry)) {
                JarInputStream jarInputStream = new JarInputStream(is);
                ZipEntry entry = null;
                while ((entry = jarInputStream.getNextEntry()) != null) {
                    entries.add(entry.getName());
                }
            }catch (IOException e){
            }
        }catch(IOException e){
        }

        return entries;
    }

    public static List<String> getJarArchiveTree(File file, boolean checksum){
        List<String> tree = new ArrayList<>();

        try(JarFile jarFile = new JarFile(file);
            CheckedInputStream cs =
                    new CheckedInputStream(new FileInputStream(file), new Adler32());
            JarInputStream jarInputStream =
                    new JarInputStream(new BufferedInputStream(cs));
        ) {
            JarEntry jarEntry = null;
            while((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                if(!jarEntry.isDirectory()) {

                    tree.add((checksum && jarEntry.getCrc() > 0) ?
                            String.join("@", jarEntry.getName(), String.valueOf(jarEntry.getCrc()))
                            : jarEntry.getName());

                    if (FileUtils.extension(jarEntry.getName()).equals("jar") ||
                            FileUtils.extension(jarEntry.getName()).equals("zip")) {
                        try (InputStream is = jarFile.getInputStream(jarEntry)) {
                            JarInputStream jis = new JarInputStream(is);
                            ZipEntry entry = null;
                            while ((entry = jis.getNextEntry()) != null) {
                                if(!entry.isDirectory()) {
                                    tree.add((checksum && entry.getCrc() > 0) ?
                                            String.join("@", ("+- " + entry.getName()), String.valueOf(entry.getCrc()))
                                            : entry.getName());
                                }
                            }
                        } catch (IOException e) {
                        }
                    }

                } // not dir
            }
        }catch(IOException e){
        }
        return tree;
    }

    /*
    在archive中搜索隐含在jar entry中文件
     */
    public static List<String> searchWithinJarArchive(File file, String criteria, boolean checksum){
        List<String> criterias = new ArrayList<>();

        try(JarFile jarFile = new JarFile(file);
            CheckedInputStream cs =
                    new CheckedInputStream(new FileInputStream(file), new Adler32());
            JarInputStream jarInputStream =
                    new JarInputStream(new BufferedInputStream(cs));
        ) {
            JarEntry jarEntry = null;
            while((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                if(!jarEntry.isDirectory()) {

                    if(jarEntry.getName().contains(criteria)) {
                        criterias.add((checksum && jarEntry.getCrc() > 0) ?
                                String.join("@", jarEntry.getName(), String.valueOf(jarEntry.getCrc()))
                                : jarEntry.getName());
                    }

                    if (FileUtils.extension(jarEntry.getName()).equals("jar") ||
                            FileUtils.extension(jarEntry.getName()).equals("zip")) {

                        try (InputStream is = jarFile.getInputStream(jarEntry)) {
                            JarInputStream jis = new JarInputStream(is);
                            ZipEntry entry = null;
                            while ((entry = jis.getNextEntry()) != null) {
                                if(!entry.isDirectory()) {
                                    if(entry.getName().contains(criteria)){
                                        criterias.add((checksum && entry.getCrc() > 0) ?
                                                String.join("\n", jarEntry.getName(), "+- "+String.join("@",entry.getName(), String.valueOf(entry.getCrc())))
                                                : String.join("\n", jarEntry.getName(), "+- "+entry.getName()));
                                    }
                                }
                            }
                        } catch (IOException e) {
                        }
                    }

                } // not dir
            }
        }catch(IOException e){
        }

        return criterias;
    }

    public static String getJarPomContent(File jarFile) {
        final String NewLine = "\n";
        String entriesContent = getJarEntryPatternContent(jarFile, "pom.xml", true);
        String jarName = FileUtils.filename(jarFile.getName()).replace("."+FileUtils.extension(jarFile.getName()), "");

        if(StringUtils.isNotBlank(entriesContent)) {
            String[] entries = entriesContent.contains(NewLine)? entriesContent.split(NewLine) : new String[]{entriesContent};

            var list = Stream.of(entries)
                    .filter(u->u.trim().endsWith(String.join("/", jarName, "pom.xml")))
                    .collect(Collectors.toList());
            if(list.size()>0) {
                return getZipEntryContent(jarFile, list.get(0));
            }
        }
        return entriesContent;
    }

    public static String getJarGroupId(File jarFile) {
        final String NewLine = "\n";
        String entriesContent = getJarEntryPatternContent(jarFile, "pom.xml", true);


        if(StringUtils.isNotBlank(entriesContent)) {
            String[] entries = entriesContent.contains(NewLine) ? entriesContent.split(NewLine) : new String[]{entriesContent};

            String pomEntry = null;
            if(entries.length>1) {
                final String jarName = FileUtils.filename(jarFile.getName())
                        .replaceAll("-[0-9\\.]+\\-?[a-zA-Z]*.jar", "");
                //remove -version-RELEASE.jar

                var list = Stream.of(entries)
                        .filter(u -> u.trim().endsWith(String.join("/", jarName, "pom.xml")))
                        .collect(Collectors.toList());
                if (list.size() ==1) {
                    pomEntry = list.get(0);
                }
            }else if(entries.length==1){
                pomEntry = entries[0];
            }

            String dirname = FileUtils.dirname(FileUtils.dirname(pomEntry));
            // get groupId from it
            return FileUtils.filename(dirname);
        }
        return "";
    }
}
