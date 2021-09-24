package com.jfeat.jar.dependency;

import org.apache.commons.lang3.StringUtils;

import javax.swing.plaf.PanelUI;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.*;

public class ZipFileUtils {

    public static void main(String[] args) throws Exception {
        String lib = "dependency-cli/lib/test.jar";
        File libFile = new File(lib);
        String libPath = libFile.getAbsolutePath();

        long checksum = UnzipWithChecksum(libPath);
        System.out.println(checksum);
    }

    public static long UnzipWithChecksum(InputStream zipStream, String pattern) throws IOException {
        try (
            // Creating input stream that also maintains the checksum of
            // the data which later can be used to validate data
            // integrity.
            CheckedInputStream cs =
                    new CheckedInputStream(zipStream, new Adler32());
            ZipInputStream zis =
                    new ZipInputStream(new BufferedInputStream(cs))) {

            ZipEntry entry=null;
            long checksum = 0;

            // Read each entry from the ZipInputStream until no more entry
            // found indicated by a null return value of the getNextEntry()
            // method.
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().matches(pattern)) {
                    long size = entry.getCrc();
                    //size = entry.getCrc();
                    checksum += size;
                    if (size > 0) {
                        byte[] buffer = new byte[1048];

                        try (FileOutputStream fos =
                                     new FileOutputStream(entry.getName());
                             BufferedOutputStream bos =
                                     new BufferedOutputStream(fos, buffer.length)) {

                            while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                                bos.write(buffer, 0, (int) size);
                            }
                            bos.flush();
                        }
                    }
                }
            }
            // Print out the checksum value
            return checksum;
        }
    }

    public static long UnzipWithChecksum(InputStream zipStream) throws IOException {
        return UnzipWithChecksum(zipStream, "");
    }

    public static long UnzipWithChecksum(File zipFile) throws IOException {
        try (InputStream fis = new FileInputStream(zipFile)) {
            return UnzipWithChecksum(fis);
        }
    }

    public static long UnzipWithChecksum(File zipFile, String pattern) throws IOException {
        try (InputStream fis = new FileInputStream(zipFile)) {
            return UnzipWithChecksum(fis, pattern);
        }
    }

    public static long UnzipWithChecksum(String zipName) throws IOException {
        try (FileInputStream fis = new FileInputStream(zipName)) {
            return UnzipWithChecksum(fis);
        }
    }

    public static long UnzipWithChecksum(String zipName, String pattern) throws IOException {
        try (FileInputStream fis = new FileInputStream(zipName)) {
            return UnzipWithChecksum(fis, pattern);
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

    public void addFileToZip(File fileToAdd, File zipFile) {
        ZipOutputStream zos = null;
        FileInputStream fis = null;
        ZipEntry ze = null;
        byte[] buffer = null;
        int len;

        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
        } catch (FileNotFoundException e) {
        }

        ze = new ZipEntry("org" + File.separator + "cfg" +
                File.separator + "resource" + File.separator + fileToAdd.getName());
        try {
            zos.putNextEntry(ze);

            fis = new FileInputStream(fileToAdd);
            buffer = new byte[(int) fileToAdd.length()];

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
    }

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
}
