package com.jfeat.jar.dependency;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.*;

import static com.jfeat.jar.dependency.FileUtils.getRelativeFilePath;

public class ZipFileUtils {

    public static void main(String[] args) throws Exception {
        String lib = "dependency-cli/lib/test.jar";
        File libFile = new File(lib);

        List<Map.Entry<String,Long>> checksums = UnzipWithChecksum(libFile);
        checksums.stream().forEach(x -> {
            System.out.println(x.toString());
        });
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
     * 在jar文件中解压文件
     * @param zipFile
     * @param entryExtension 文件后缀类型过滤
     * @param entryPattern 符合条件的搜索 （是否包含）
     * @param targetPath  解压到目标目录
     * @return
     * @throws IOException
     */
    public static List<String> unzipFilesFromArchiva(File zipFile, String entryExtension, String entryPattern, File targetPath) throws IOException {
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
            List<String> files = new ArrayList<>();

            // Read each entry from the ZipInputStream until no more entry
            // found indicated by a null return value of the getNextEntry()
            // method.
            while ((entry = zis.getNextEntry()) != null) {
                if ( (StringUtils.isBlank(entryExtension) || entryExtension.equals(FileUtils.extension(entry.getName()))) &&
                        (StringUtils.isBlank(entryPattern) || entry.getName().contains(entryPattern))
                ) {
                    long size = entry.getCrc();
                    String relativePath = getRelativeFilePath(zipFile, targetPath);
                    String filename = targetPath!=null? (String.join(File.separator, relativePath, FileUtils.filename(entry.getName()))) : entry.getName();

                    if (size > 0) {
                        String dirname = FileUtils.dirname(zipFile.getAbsolutePath());
                        String entryFullName = String.join(File.separator,dirname, (targetPath!=null? filename: entry.getName()) );
                        FileUtils.mkdir(FileUtils.dirname(entryFullName));

                        byte[] buffer = new byte[1048];
                        try (FileOutputStream fos =
                                     new FileOutputStream(entryFullName);
                             BufferedOutputStream bos =
                                     new BufferedOutputStream(fos, buffer.length)) {

                            while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                                bos.write(buffer, 0, (int) size);
                            }
                            bos.flush();
                        }
                    }
                    files.add(filename);
                }
            }

            // Print out the checksum value
            return files;
        }
    }

    public static List<Map.Entry<String,Long>> UnzipWithChecksum(File zipFile) throws IOException {
        return UnzipWithChecksum(zipFile, "", "", null);
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
     * 在zipFile中解压匹配pattern的文件
     * @param zipFile
     * @param entryExtension 文件后缀过滤条件
     * @param entryPattern 包含此字符串的文件
     * @param targetPath  解压到目标目录 (绝对路径)
     * @return
     * @throws IOException
     */
    public static List<Map.Entry<String,Long>> UnzipWithChecksum(File zipFile, String entryExtension, String entryPattern, File targetPath) throws IOException {
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
                String entryFilename = FileUtils.filename(entry.getName().replace('/', File.separatorChar));
                String extension = FileUtils.extension(entryFilename);

                if ( (StringUtils.isBlank(entryExtension) || entryExtension.equals(extension)) &&
                        (StringUtils.isBlank(entryPattern) || entry.getName().contains(entryPattern))
                ) {
                    long size = entry.getCrc();

                    //String targetFilename = StringUtils.isNotBlank(target)? (String.join(File.separator, target, entryFilename)) : entry.getName().replace('/', File.separatorChar);
                    String targetFilename = targetPath==null? entry.getName().replace('/', File.separatorChar) : (String.join(File.separator, targetPath.getAbsolutePath(), entryFilename));

                    Map.Entry<String,Long> checksum = Map.entry(targetFilename, size);
                    //if (size > 0) {
                        String entryFullName = targetPath!=null?
                                (String.join(File.separator, getRelativeFilePath(zipFile, targetPath), entryFilename)) :
                                (String.join(File.separator, FileUtils.dirname(zipFile.getAbsolutePath()), entryFilename));

                                File entryDirFile = new File(FileUtils.dirname(entryFullName));
                        if(!entryDirFile.exists()){
                            FileUtils.forceMkdir(entryDirFile);
                        }

                        if(entryDirFile.exists()) {
                            byte[] buffer = new byte[1048];
                            try (FileOutputStream fos =
                                         new FileOutputStream(entryFullName);
                                 BufferedOutputStream bos =
                                         new BufferedOutputStream(fos, buffer.length)) {

                                while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                                    bos.write(buffer, 0, (int) size);
                                }
                                bos.flush();
                            }

                            // success
                            checksums.add(checksum);

                        }else{
                            throw new IOException("fail to create dictionary: " + entryDirFile);
                        }
                    //}
                }
            }

            // Print out the checksum value
            return checksums;
        }
    }


    //https://www.cnblogs.com/softidea/p/4272451.html
    //http://stackoverflow.com/questions/3048669/how-can-i-add-entries-to-an-existing-zip-file-in-java?lq=1

    public static void addZipEntry() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");

        Path path = Paths.get("test.zip");
        URI uri = URI.create("jar:" + path.toUri());

        try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            Path nf = fs.getPath("new.txt");
            try (Writer writer = Files.newBufferedWriter(nf, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
                writer.write("hello");
            }
        }
    }

    public static String addFileToZip(File zipFile, File addFile) {
        ZipOutputStream zos = null;
        FileInputStream fis = null;
        ZipEntry ze = null;
        byte[] buffer = null;
        int len;

        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
        } catch (FileNotFoundException e) {
        }

        String entryPath = getRelativeFilePath(zipFile, addFile);
        ze = new ZipEntry(entryPath);
        try {
            zos.putNextEntry(ze);

            fis = new FileInputStream(addFile);
            buffer = new byte[(int) addFile.length()];

            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        } catch (IOException e) {
        }
        try {
            zos.flush();
            zos.close();
            fis.close();
        } catch (IOException e) {
        }
        return entryPath;
    }

    // https://stackoverflow.com/questions/2223434/appending-files-to-a-zip-file-with-java
    public static void addFilesToZip(File source, File[] files) {
        try {
            File tmpZip = File.createTempFile(source.getName(), null);
            tmpZip.delete();
            if (!source.renameTo(tmpZip)) {
                throw new Exception("Could not make temp file (" + source.getName() + ")");
            }
            byte[] buffer = new byte[1024];
            ZipInputStream zin = new ZipInputStream(new FileInputStream(tmpZip));
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(source));

            for (int i = 0; i < files.length; i++) {
                InputStream in = new FileInputStream(files[i]);
                out.putNextEntry(new ZipEntry(files[i].getName()));
                for (int read = in.read(buffer); read > -1; read = in.read(buffer)) {
                    out.write(buffer, 0, read);
                }
                out.closeEntry();
                in.close();
            }

            for (ZipEntry ze = zin.getNextEntry(); ze != null; ze = zin.getNextEntry()) {
                out.putNextEntry(ze);
                for (int read = zin.read(buffer); read > -1; read = zin.read(buffer)) {
                    out.write(buffer, 0, read);
                }
                out.closeEntry();
            }

            out.close();
            tmpZip.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * sample
     * @param args
     * @throws Exception
     */
    //https://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
    public static void addFileToZipFS(String[] args) throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:file:/codeSamples/zipfs/zipfstest.zip");

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path externalTxtFile = Paths.get("/codeSamples/zipfs/SomeTextFile.txt");
            Path pathInZipfile = zipfs.getPath("/SomeTextFile.txt");

            // copy a file into the zip file
            Files.copy(externalTxtFile, pathInZipfile,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void checkManifest(String jarFileName, String mainClass) throws Exception {
        File f = new File(jarFileName);
        ZipFile zf = new ZipFile(f);
        ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");
        BufferedReader r = new BufferedReader(
                new InputStreamReader(zf.getInputStream(ze)));
        String line = r.readLine();
        while (line != null && !(line.startsWith("Main-Class:"))) {
            line = r.readLine();
        }
        zf.close();
    }
}
