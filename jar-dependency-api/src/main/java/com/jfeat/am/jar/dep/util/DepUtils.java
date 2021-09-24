package com.jfeat.am.jar.dep.util;

import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import com.jfeat.jar.dependency.DependencyUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    /**
     * pre deploy, convert jar file to BOOT-INF/lib
     * @param jarFile
     * @return
     */
    public static File convertToLibJar(File jarFile, String rootPath) {
        final String LIB = "BOOT-INF/lib";
        File libFile = new File(rootPath + File.separator + LIB + File.separator + jarFile.getName());
        if(libFile.exists()) {
            try {
                FileUtils.forceDelete(libFile);
                FileUtils.moveFile(jarFile, libFile);
            } catch (IOException e) {
                throw new BusinessException(BusinessCode.GeneralIOError, "delete file error: " +e.getMessage());
            }
        }else{
            try {
                FileUtils.moveFile(jarFile, libFile);
            } catch (IOException e) {
                throw new BusinessException(BusinessCode.GeneralIOError, "move file error:"+e.getMessage());
            }
        }
        return libFile;
    }
}
