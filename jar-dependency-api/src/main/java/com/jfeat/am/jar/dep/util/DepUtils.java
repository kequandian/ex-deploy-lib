package com.jfeat.am.jar.dep.util;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import com.jfeat.jar.dependency.DependencyUtils;
import com.jfeat.jar.dependency.ZipFileUtils;
import org.apache.commons.io.FileUtils;
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
     * get single file checksum
     * @param file
     * @param hashCode
     * @return
     * @throws IOException
     */
    public static long getFileChecksumAsLong(File file, String hashCode) throws IOException{
        return getFileChecksumCode(file, hashCode).padToLong();
    }
    public static String getFileChecksum(File file, String hashCode) throws IOException{
        return getFileChecksumCode(file, hashCode).toString();
    }
    public static HashCode getFileChecksumCode(File file, String hashCode) throws IOException{
        HashCode checksumCode = Files.hash(file, Hashing.md5());
        if(StringUtils.isNotEmpty(hashCode)) {
            final String[] supportedType  =new String[]{"adler32","crc32","crc32c","md5","sha1","sha256","sha512"};
            Assert.isTrue(Stream.of(supportedType).collect(Collectors.toList()).contains(hashCode),
                    "supported type: " + String.join(",", supportedType));
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
        var query = ZipFileUtils.listEntriesFromArchive(jarFile, filename);
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
     *
     * @param rootPath the root path
     * @param dir  the jar location
     * @param jar  the jar
     * @param pattern  filter the file within jar
     * @param target  extra files to
     * @return
     */
    public static List<Map.Entry<String,Long>> extraFilesFromJar(String rootPath, String dir, String jar, String pattern, String target) throws IOException{
        File rootJarFile = new File(String.join(File.separator, rootPath, dir, jar));

        if(StringUtils.isNotEmpty(target)){
            String targetPath = String.join(File.separator, rootPath, target);
            if(!new File(targetPath).exists()){
                org.codehaus.plexus.util.FileUtils.mkdir(targetPath);
            }
        }

        return ZipFileUtils.UnzipWithChecksum(rootJarFile, pattern, target);
    }
}
