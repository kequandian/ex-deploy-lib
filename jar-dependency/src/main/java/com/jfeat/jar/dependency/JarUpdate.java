package com.jfeat.jar.dependency;

import org.codehaus.plexus.util.FileUtils;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
//https://www.developer.com/guides/accessing-zips-and-jars-using-java-part-2/
// http://thornton.rosesquared.org/articles/ZipJar/part2.html

public class JarUpdate {

    public static String getRelativeFilePath(File baseFile, File file) {
        String entryPath = org.codehaus.plexus.util.FileUtils.dirname(file.getAbsolutePath())
                .substring(org.codehaus.plexus.util.FileUtils.dirname(baseFile.getAbsolutePath()).length() + 1);

        return String.join(File.separator, entryPath, org.codehaus.plexus.util.FileUtils.filename(file.getAbsolutePath()))
                .replace(File.separator, "/");
    }

    /**
     * main()
     */
    public static String addFile(File jarFile, File inputFile) throws IOException {
        // Create file descriptors for the jar and a temp jar.
        File tempJarFile = new File(jarFile.getAbsolutePath() + ".tmp");

        // get input fileName
        String fileName = getRelativeFilePath(jarFile, inputFile);


        // Open the jar file.
        JarFile jar = new JarFile(jarFile);
        System.out.println(jarFile.getName() + " opened.");

        // Initialize a flag that will indicate that the jar was updated.

        boolean jarUpdated = false;

        try {
            // Create a temp jar file with no manifest. (The manifest will
            // be copied when the entries are copied.)

            Manifest jarManifest = jar.getManifest();
            JarOutputStream tempJar =
                    new JarOutputStream(new FileOutputStream(tempJarFile));

            // Allocate a buffer for reading entry data.

            byte[] buffer = new byte[1024];
            int bytesRead;

            try {
                // Open the given file.

                FileInputStream fis = new FileInputStream(inputFile);

                try {
                    // Create a jar entry and add it to the temp jar.

                    JarEntry entry = new JarEntry(fileName);
                    entry.setMethod(ZipEntry.STORED);
                    tempJar.putNextEntry(entry);

                    // Read the file and write it to the jar.

                    int bytesReadTotal = 0;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        tempJar.write(buffer, 0, bytesRead);
                        bytesReadTotal += bytesRead;
                    }
                    entry.setSize(bytesReadTotal);
                    entry.setCrc(0);
                    //CRC32 crc = new CRC32();
                    //crc.reset();
                    //crc.update(buffer);
                    //entry.setCrc(crc.getValue());

                    System.out.println(entry.getName() + " added.");
                }
                finally {
                    fis.close();
                }

                // Loop through the jar entries and add them to the temp jar,
                // skipping the entry that was added to the temp jar already.

                for (Enumeration entries = jar.entries(); entries.hasMoreElements(); ) {
                    // Get the next entry.

                    JarEntry entry = (JarEntry) entries.nextElement();

                    // If the entry has not been added already, add it.

                    if (! entry.getName().equals(fileName)) {
                        // Get an input stream for the entry.

                        InputStream entryStream = jar.getInputStream(entry);

                        // Read the entry and write it to the temp jar.

                        tempJar.putNextEntry(entry);

                        while ((bytesRead = entryStream.read(buffer)) != -1) {
                            tempJar.write(buffer, 0, bytesRead);
                        }
                    }
                }

                jarUpdated = true;
            }
            catch (Exception ex) {
                System.out.println(ex);

                // Add a stub entry here, so that the jar will close without an
                // exception.

                tempJar.putNextEntry(new JarEntry("stub"));
            }
            finally {
                tempJar.close();
            }
        }
        finally {
            jar.close();
            System.out.println(jarFile.getName() + " closed.");

            // If the jar was not updated, delete the temp jar file.

            if (! jarUpdated) {
                tempJarFile.delete();
            }
        }

        // If the jar was updated, delete the original jar file and rename the
        // temp jar file to the original name.

        if (jarUpdated) {
            jarFile.renameTo(new File(jarFile.getAbsolutePath().replace(".jar", ".zip.temp")));
            tempJarFile.renameTo(jarFile);
            System.out.println(jarFile.getName() + " updated.");
        }

        return fileName;
    }
}