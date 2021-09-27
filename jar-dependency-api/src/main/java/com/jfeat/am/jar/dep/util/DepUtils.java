package com.jfeat.am.jar.dep.util;

import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import com.jfeat.crud.base.tips.SuccessTip;
import com.jfeat.jar.dependency.DependencyUtils;
import com.jfeat.jar.dependency.ZipFileUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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


    /**
     * pre deploy, convert deploying file to the same path on jar
     * @param jarFile
     * @return
     */
    public static File alignJarEntry(File jarFile, File deployingFile) throws IOException {
        Assert.isTrue(jarFile.exists(), jarFile + " not exists!");
        Assert.isTrue(deployingFile.exists(), deployingFile + " not exists!");

        // locate file with jar
        String filename = org.codehaus.plexus.util.FileUtils.filename(deployingFile.getName());
        var query = ZipFileUtils.listFilesFromArchive(jarFile, filename);
        Assert.isTrue(query.size()==1, "fail to find deploying file within jar:" + filename);

        // deploy
        String targetJarDir = org.codehaus.plexus.util.FileUtils.dirname(jarFile.getAbsolutePath());
        String targetFilename = query.get(0);

        String targetDeploy = String.join(File.separator, targetJarDir, targetFilename);
        String targetDeployDirname = org.codehaus.plexus.util.FileUtils.dirname(targetDeploy);
        if(!new File(targetDeployDirname).exists()){
            org.codehaus.plexus.util.FileUtils.mkdir(targetDeploy);
        }

        //move deploying file as target filename
        var result = new File(targetDeploy);
        if(result.exists()){
            FileUtils.forceDelete(result);
        }
        FileUtils.moveFile(deployingFile, result);

        return result;
    }
}
