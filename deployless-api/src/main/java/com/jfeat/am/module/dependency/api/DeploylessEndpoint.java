package com.jfeat.am.module.dependency.api;

import com.alibaba.fastjson.JSONArray;
import com.jfeat.am.module.dependency.properties.DeploylessProperties;
import com.jfeat.am.module.dependency.services.persistence.dto.JarDTO;
import com.jfeat.am.module.dependency.services.persistence.model.Jar;
import com.jfeat.am.module.dependency.services.service.impl.JarServiceImpl;
import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import com.jfeat.crud.base.tips.ErrorTip;
import com.jfeat.crud.base.tips.SuccessTip;
import com.jfeat.crud.base.tips.Tip;
import com.jfeat.fs.api.FileServiceEndpoint;
import com.jfeat.fs.util.FileInfo;
import com.jfeat.jar.dependency.DependencyUtils;
import com.jfeat.jar.dependency.ZipFileUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import java.util.UUID;
import java.util.zip.ZipFile;

import static com.jfeat.fs.api.FileServiceEndpoint.getExtensionName;

/**
 * 依赖处理接口
 *
 * @author zxchengb
 * @date 2020-08-05
 */
@RestController
@Api("/api/jar/dependencies")
@RequestMapping("/api/jar/deploy")
public class DeploylessEndpoint {
    protected final static Logger logger = LoggerFactory.getLogger(DeploylessEndpoint.class);

    @Autowired
    private DeploylessProperties deploylessProperties;

    @GetMapping("/show")
    @ApiOperation(value = "获取待装配的lib文件列表", response = Jar.class)
    public Tip getLibs() {
        String appsPath = deploylessProperties.getAppsPath();
        String libPath = deploylessProperties.getLibPath();
        String libWorkPath = appsPath + File.separator + libPath;

        ArrayList<String> emptyArray  =new ArrayList<>();

        File libPathFile = new File(libWorkPath);
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

    @PostMapping("/send")
    @ApiOperation(value = "装配lib.jar至应用standalone.jar", response = HashMap.class)
    public Tip greenFieldLib(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(BusinessCode.BadRequest, "file is empty");
        }
        return SuccessTip.create(new ArrayList<String>());
    }


    @PostMapping
    @ApiOperation(value = "装配lib.jar至应用standalone.jar", response = HashMap.class)
    public Tip deployLib(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(BusinessCode.BadRequest,  "file is empty");
        }
        String appsPath = deploylessProperties.getAppsPath();
        String libPath = deploylessProperties.getLibPath();

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
                File appFile = deploylessProperties.getStandaloneFile();
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