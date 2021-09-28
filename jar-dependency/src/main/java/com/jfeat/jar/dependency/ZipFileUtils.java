package com.jfeat.jar.dependency;

import com.jfeat.jar.dependency.model.JarModel;
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

        List<JarModel> checksums = UnzipWithChecksum(libFile);
        checksums.stream().forEach(x -> {
            System.out.println(x.toString());
        });
    }


    public static List<String> listFilesFromArchive(File zipFile, String pattern) throws IOException{
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
            List<String> list = new ArrayList<>();

            while ((entry = zis.getNextEntry()) != null) {
                if (StringUtils.isBlank(pattern) || entry.getName().contains(pattern)) {
                    list.add(entry.getName());
                }
            }
            return list;
        }
    }

    /**
     * 在jar文件中解压文件
     * @param zipFile
     * @param pattern 符合条件的搜索 （是否包含内容）
     * @param target  解压到目标目录
     * @return
     * @throws IOException
     */
    public static List<String> unzipFilesFromArchiva(File zipFile, String pattern, String target) throws IOException {
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
                if (StringUtils.isBlank(pattern) || entry.getName().contains(pattern)) {
                    long size = entry.getCrc();
                    String filename = StringUtils.isNotBlank(target)? (String.join(File.separator, target, FileUtils.filename(entry.getName()))) : entry.getName();

                    if (size > 0) {
                        String dirname = FileUtils.dirname(zipFile.getAbsolutePath());
                        String entryFullName = String.join(File.separator,dirname,
                                ( StringUtils.isNotBlank(target)? filename: entry.getName()) );
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

    public static List<JarModel> UnzipWithChecksum(File zipFile) throws IOException {
        return UnzipWithChecksum(zipFile, "", "");
    }



    /**
     * 在zipFile中查找文件 pattern
     * @param zipFile
     * @param pattern 符合条件的搜索 （是否包含内容）
     * @param target  解压到目标目录
     * @return
     * @throws IOException
     */
    public static List<JarModel> UnzipWithChecksum(File zipFile, String pattern, String target) throws IOException {
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
            List<JarModel> checksums = new ArrayList<>();

            // Read each entry from the ZipInputStream until no more entry
            // found indicated by a null return value of the getNextEntry()
            // method.
            while ((entry = zis.getNextEntry()) != null) {
                if (StringUtils.isBlank(pattern) || entry.getName().contains(pattern)) {
                    JarModel jarModel = new JarModel();
                    long size = entry.getCrc();
                    jarModel.setChecksum(size);
                    String entryFilename = FileUtils.filename(entry.getName().replace('/', File.separatorChar));
                    String filename = StringUtils.isNotBlank(target)? (String.join(File.separator, target, entryFilename)) : entry.getName();
                    jarModel.setJar(filename);

                    //if (size > 0) {
                        String dirname = FileUtils.dirname(zipFile.getAbsolutePath());
                        String entryFullName = String.join(File.separator,dirname,
                                ( StringUtils.isNotBlank(target)? filename: entry.getName()) );
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

                        checksums.add(jarModel);
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
