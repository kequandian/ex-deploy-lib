package com.jfeat.am.jar.deploy.api;

import com.alibaba.fastjson.JSONArray;
import com.jfeat.am.jar.deploy.properties.JarDeployProperties;
import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import com.jfeat.crud.base.tips.SuccessTip;
import com.jfeat.crud.base.tips.Tip;
import com.jfeat.jar.dependency.DependencyUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 依赖处理接口
 *
 * @author zxchengb
 * @date 2020-08-05
 */
@RestController
@Api("/api/jar/deploy")
@RequestMapping("/api/jar/deploy")
public class JarDeployEndpoint {
    protected final static Logger logger = LoggerFactory.getLogger(JarDeployEndpoint.class);

    @Autowired
    private JarDeployProperties jarDeployProperties;

    @GetMapping("/libs")
    @ApiOperation(value = "获取待装配的lib文件列表")
    public Tip getLibs() {
        String libPath = jarDeployProperties.getLibPath();
        ArrayList<String> emptyArray  =new ArrayList<>();

        File libPathFile = new File(libPath);
        String fullPath =  libPathFile.getAbsolutePath();
        logger.info(fullPath);

        if(!libPathFile.exists()){
            return SuccessTip.create(emptyArray);
        }
        File[] libsFiles = libPathFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return FilenameUtils.getExtension(s).equals("jar");
            }
        });

        for(File f : libsFiles){
            emptyArray.add(f.getName());
        }

        return SuccessTip.create(emptyArray);
    }
    

    @PostMapping("/lib/send")
    @ApiOperation(value = "发送lib至目录", response = HashMap.class)
    public Tip sendLib(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(BusinessCode.BadRequest, "file is empty");
        }
        String libPath = jarDeployProperties.getLibPath();
        File libPathFile = new File(libPath);
        if (!libPathFile.exists()) {
            libPathFile.mkdirs();
        }

        String originalFileName = file.getOriginalFilename();
        String extensionName = FilenameUtils.getExtension(originalFileName);
        Long fileSize = file.getSize();
        if(fileSize==0){
            throw new BusinessException(BusinessCode.BadRequest,  "file is empty");
        }

        try {
            File target = new File(libPath + File.separator + originalFileName);
            String path = target.getCanonicalPath();
            boolean readable = target.setReadable(true);
            if(readable){
                logger.info("file uploading to: {}", path);
                FileUtils.copyInputStreamToFile(file.getInputStream(), target);
                logger.info("file uploaded to: {}", target.getAbsolutePath());
            }else{
                throw new BusinessException(BusinessCode.UploadFileError, "file is not readable");
            }
        } catch (Exception e) {
            throw new BusinessException(BusinessCode.UploadFileError);
        }

        return SuccessTip.create(new ArrayList<String>());
    }


    @GetMapping("/dependencies")
    @ApiOperation(value = "查询lib的依赖", response = HashMap.class)
    public Tip queryLibDependencies(@RequestParam("lib") String libFileName) {
        String libPath = jarDeployProperties.getLibPath();

        File target = new File(libPath + File.separator + libFileName);
        boolean readable = target.setReadable(true);
        if(readable){
            List<String> libDependencies = DependencyUtils.getDependenciesByJar(target);
            return SuccessTip.create(libDependencies);
        }else{
            throw new BusinessException(BusinessCode.UploadFileError, "file is not readable");
        }
    }

    @GetMapping("/dependencies/app")
    @ApiOperation(value = "查询standalone的依赖", response = JSONArray.class)
    public Tip queryAppDependencies() {
        String appPath = jarDeployProperties.getAppsPath();
        File appPathFile = new File(appPath);

        // get app.jar or *-standalone.jar
        File[] libsFiles = appPathFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.equals("app.jar") ||
                        s.endsWith("-standalone.jar");
            }
        });
        ArrayList<String> emptyArray  =new ArrayList<>();
        if(libsFiles==null || libsFiles.length==0){
            return SuccessTip.create(emptyArray);
        }
        if(libsFiles.length>1){
            for(File ss : libsFiles){
                emptyArray.add(ss.getName());
            }
            return SuccessTip.create(emptyArray);
        }

        File target = libsFiles[0];
        boolean readable = target.setReadable(true);
        if(readable){
            List<String> libDependencies = DependencyUtils.getDependenciesByJar(target);
            return SuccessTip.create(libDependencies);
        }else{
            throw new BusinessException(BusinessCode.UploadFileError, "file is not readable");
        }
    }

    @GetMapping("/lib/sanity")
    @ApiOperation(value = "检查lib是否可装配", response = JSONArray.class)
    public Tip queryLibSanity(@RequestParam("lib") String libFileName) {
        String libPath = jarDeployProperties.getLibPath();

        try {
            File target = new File(libPath + File.separator + libFileName);
            String path = target.getCanonicalPath();
            boolean readable = target.setReadable(true);
            if(readable){
                File appFile = jarDeployProperties.getStandaloneFile();
                if(appFile!=null) {
                    List<String> appDependencies = DependencyUtils.getDependencies(appFile.getAbsolutePath());
                    List<String> libDependencies = DependencyUtils.getDependenciesByJar(target);
                    List<String> diffDependencies = DependencyUtils.getDifferentDependencies(appDependencies, libDependencies);

                    if (diffDependencies != null && diffDependencies.size() > 0) {
                        // mismatch, just delete the file
                        FileUtils.forceDelete(target);
                        return SuccessTip.create(diffDependencies);
                    }
                }
            }else{
                throw new BusinessException(BusinessCode.FileReadingError, "file is not readable");
            }

        } catch (Exception e) {
            throw new BusinessException(BusinessCode.FileReadingError);
        }

        return SuccessTip.create(new ArrayList<String>());
    }


    @PostMapping("/app/depand/lib")
    @ApiOperation(value = "装配lib.jar至应用standalone.jar", response = HashMap.class)
    public Tip deployLib(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(BusinessCode.BadRequest,  "file is empty");
        }
        String appsPath = jarDeployProperties.getAppsPath();
        String libPath = jarDeployProperties.getLibPath();

        String originalFileName = file.getOriginalFilename();
        String extensionName = FilenameUtils.getExtension(originalFileName);
        Long fileSize = file.getSize();
        if(fileSize==0){
            throw new BusinessException(BusinessCode.BadRequest,  "file is empty");
        }

        try {
            String fileSavePath = appsPath + File.separator + libPath;
            File fileSavePath_f = new File(fileSavePath);
            if (!fileSavePath_f.exists()) {
                fileSavePath_f.mkdirs();
            }

            File target = new File(fileSavePath + File.separator + originalFileName);
            String path = target.getCanonicalPath();
            boolean readable = target.setReadable(true);
            if(readable){
                logger.info("file uploading to: {}", path);
                FileUtils.copyInputStreamToFile(file.getInputStream(), target);
                logger.info("file uploaded to: {}", target.getAbsolutePath());

                // check dependencies
                File appFile = jarDeployProperties.getStandaloneFile();
                List<String> appDependencies = DependencyUtils.getDependencies(appFile.getAbsolutePath());
                List<String> libDependencies = DependencyUtils.getDependenciesByJar(target);
                List<String> diffDependencies = DependencyUtils.getDifferentDependencies(appDependencies, libDependencies);

                if(diffDependencies!=null && diffDependencies.size()>0) {
                    // mismatch, just delete the file
                    FileUtils.forceDelete(target);
                    return SuccessTip.create(diffDependencies);
                }

                // allow inject
                // just wait for cron to deploy the lib
                //ZipFileUtils.addFilesToZip(appFile, new File[]{target});

            }else{
                throw new BusinessException(BusinessCode.UploadFileError, "file is not readable");
            }
        } catch (Exception e) {
            throw new BusinessException(BusinessCode.UploadFileError);
        }

        //return SuccessTip.create(JSONArray.parseArray("[]"));
        return SuccessTip.create(new ArrayList<String>());
    }
}