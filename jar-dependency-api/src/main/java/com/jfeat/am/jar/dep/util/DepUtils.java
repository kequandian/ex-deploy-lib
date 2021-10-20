package com.jfeat.am.jar.dep.util;

import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import com.jfeat.jar.dependency.DependencyUtils;
import com.jfeat.jar.dependency.JarUpdate;
import com.jfeat.jar.dependency.ZipFileUtils;
import com.jfeat.jar.dependency.comparable.ChecksumKeyValue;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DepUtils {
    public static List<String> getMismatchJars(String rootPath, String baseJar, String jar, boolean skipVersion){
        File rootJarFile = new File(rootPath + File.separator + baseJar);
        if(!rootJarFile.exists()){
            throw new BusinessException(BusinessCode.FileNotFound);
        }
        if(!rootJarFile.setReadable(true)){
            throw new BusinessException(BusinessCode.FileReadingError);
        }
        File jarFile = new File(rootPath + File.separator + jar);
        if(!jarFile.exists()){
            throw new BusinessException(BusinessCode.FileNotFound);
        }
        if(!jarFile.setReadable(true)){
            throw new BusinessException(BusinessCode.FileReadingError);
        }

        List<String> appDependencies = DependencyUtils.getDependenciesByJar(rootJarFile);
        List<String> libDependencies = DependencyUtils.getDependenciesByJar(jarFile);
        return skipVersion ? DependencyUtils.getDifferentDependenciesIgnoreVersion(appDependencies, libDependencies)
                : DependencyUtils.getDifferentDependencies(appDependencies, libDependencies);
    }

    public static List<String> getMatchJars(String rootPath, String baseJar, String jar){
        File rootJarFile = new File(rootPath + File.separator + baseJar);
        if(!rootJarFile.exists()){
            throw new BusinessException(BusinessCode.FileNotFound);
        }
        if(!rootJarFile.setReadable(true)){
            throw new BusinessException(BusinessCode.FileReadingError);
        }
        File jarFile = new File(rootPath + File.separator + jar);
        if(!jarFile.exists()){
            throw new BusinessException(BusinessCode.FileNotFound);
        }
        if(!jarFile.setReadable(true)){
            throw new BusinessException(BusinessCode.FileReadingError);
        }

        List<String> appDependencies = DependencyUtils.getDependenciesByJar(rootJarFile);
        List<String> libDependencies = DependencyUtils.getDependenciesByJar(jarFile);

        return  DependencyUtils.getSameDependencies(appDependencies, libDependencies);
    }

    public static List<Map.Entry<String,String>> getMatchJars(String rootPath, String baseJar, String jar, boolean skipVersion){
        skipVersion=true;

        File rootJarFile = new File(rootPath + File.separator + baseJar);
        if(!rootJarFile.exists()){
            throw new BusinessException(BusinessCode.FileNotFound);
        }
        if(!rootJarFile.setReadable(true)){
            throw new BusinessException(BusinessCode.FileReadingError);
        }
        File jarFile = new File(rootPath + File.separator + jar);
        if(!jarFile.exists()){
            throw new BusinessException(BusinessCode.FileNotFound);
        }
        if(!jarFile.setReadable(true)){
            throw new BusinessException(BusinessCode.FileReadingError);
        }

        List<String> appDependencies = DependencyUtils.getDependenciesByJar(rootJarFile);
        List<String> libDependencies = DependencyUtils.getDependenciesByJar(jarFile);

        return DependencyUtils.getSameDependenciesIgnoreVersion(appDependencies,libDependencies);
    }

    private static String getFullPathname(File jarFile){
        return String.join(File.separator, org.codehaus.plexus.util.FileUtils.dirname(jarFile.getAbsolutePath()), org.codehaus.plexus.util.FileUtils.filename(jarFile.getAbsolutePath()));
    }

    /**
     * pre deploy, convert deploying file to the same path on jar
     * @param jarFile
     * @return
     */
    @Deprecated
    public static File alignFileJarEntry(File jarFile, File deployingFile) throws IOException {
        Assert.isTrue(jarFile.exists(), jarFile + " not exists!");
        Assert.isTrue(deployingFile.exists(), deployingFile + " not exists!");

        // locate file with jar
        String filename = org.codehaus.plexus.util.FileUtils.filename(deployingFile.getName());
        var query = ZipFileUtils.listEntriesFromArchive(jarFile, "", filename);
        Assert.isTrue(query.size()==1, "fail to find deploying file within jar:" + filename);
        String targetFilename = query.get(0);

        // deploy
        String targetJarDir = org.codehaus.plexus.util.FileUtils.dirname(jarFile.getAbsolutePath());
        String targetJarFilename = String.join(File.separator, targetJarDir, targetFilename);
        // check if is the same file
        if(getFullPathname(deployingFile).equals(targetJarFilename)){
            return new File(targetJarFilename);
        }

        // mkdir
        {
            String targetDeployDirname = org.codehaus.plexus.util.FileUtils.dirname(targetJarFilename);
            if (!new File(targetDeployDirname).exists()) {
                org.codehaus.plexus.util.FileUtils.mkdir(targetJarFilename);
            }
        }

        //move deploying file as target filename
        var targetJarFile = new File(targetJarFilename);
        if(targetJarFile.exists()){
            FileUtils.forceDelete(targetJarFile);
        }

        FileUtils.copyFile(deployingFile, targetJarFile);

        return targetJarFile;
    }


    /**
     *  extra files from .jar file
     * @param jarFile  the jar file to extra from
     * @param entryExtension  the entry file extension, eg. .class, .jar etc.
     * @param entryPattern  the entry file name which contains
     * @param entryPattern  the target path where extra files to be
     * @return
     */
    public static List<Map.Entry<String,Long>> extraFilesFromJar(File jarFile, String entryExtension, String entryPattern, File targetPath) throws IOException{
        if(!targetPath.exists()){
            org.codehaus.plexus.util.FileUtils.mkdir(targetPath.getAbsolutePath());
        }

        var checkums = ZipFileUtils.UnzipWithChecksum(jarFile, entryExtension, entryPattern, targetPath);
        String commonBasedir = com.jfeat.jar.dependency.FileUtils.getCommonBasedir(jarFile.getAbsolutePath(), targetPath.getAbsolutePath());

        // remove rootPath from checksum
        return checkums.stream()
                // remove rootPath
                .map(entry->new ChecksumKeyValue<String,Long>(
                        StringUtils.stripStart(entry.getKey().replace(commonBasedir, ""), File.separator)
                        ,
                        entry.getValue()))
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<String> deployFilesToJar(File dirPath, String fileExtension, String filePattern, File jarFile) throws IOException{
        Assert.isTrue(dirPath.exists(), dirPath.getName() + " not exists!");

        // dir/javaclass -> classes/javaname.class
        // target to .jar
        List<File> classes = new ArrayList<>();

        if(StringUtils.isNotBlank(filePattern)){

            File[] listOfFiles = dirPath.listFiles();
            Stream.of(listOfFiles)
                    .filter(f -> FilenameUtils.getExtension(f.getName()).equals(fileExtension))
                    .filter(f -> f.getName().contains(filePattern))
                    .map(
                            f ->{
                                return new File(String.join(File.separator, dirPath.getAbsolutePath(), f.getName()));
                            }
                    )
                    .forEach(f->{
                        classes.add(f);
                    });
        }

        // update into zip/jar
        //String result = ZipFileUtils.addFileToZip(jarFile, okClassFile);
        //long crc32=Files.hash(okClassFile, Hashing.adler32()).padToLong();
        //
        // map jar entry names from jar file
        String fileTypeExtension = "."+fileExtension; //=> ".class" or ".jar"
        var entryNames =
                ZipFileUtils.listEntriesFromArchive(jarFile, fileTypeExtension, filePattern)
                        .stream()
                        .collect(Collectors.toMap(
                                entry->org.codehaus.plexus.util.FileUtils.filename(entry.replace("/", File.separator)),
                                entry->entry));

        // convert filenames to jar entry names
        var entries = classes.stream().map(file -> {
            String filename = org.codehaus.plexus.util.FileUtils.filename(file.getName());
            return entryNames.get(filename);
        }).collect(Collectors.toList());

        return JarUpdate.addFiles(jarFile, classes, entries);
    }


    public static List<String> deployFilesToJarEntry(File dirPath, String fileExtension, String filePattern, File jarFile, String jarEntryPattern) throws IOException {
        Assert.isTrue(StringUtils.isNotBlank(jarEntryPattern), "jar entry cannot be empty!");

        String libDir = "lib";
        String basedir = com.jfeat.jar.dependency.FileUtils.getCommonBasedir(dirPath.getAbsolutePath(), jarFile.getAbsolutePath());
        File entryPath = new File(String.join(File.separator, basedir, libDir));

        List<Map.Entry<String,Long>> checksums =  DepUtils.extraFilesFromJar(jarFile, "jar", jarEntryPattern, entryPath);
        Assert.isTrue(checksums.size()==1, "multi (or no) jar entries found, should be unique!");

        File entryFile = new File(String.join(File.separator, checksums.get(0).getKey()));

        var sink = deployFilesToJar(dirPath, fileExtension, filePattern, entryFile);

        return deployFilesToJar(entryPath, "jar", jarEntryPattern, jarFile);
    }
}
