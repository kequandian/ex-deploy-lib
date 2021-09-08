package com.jfeat.am.jar.deploy.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfeat.am.jar.deploy.properties.JarDeployProperties;
import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import com.jfeat.crud.base.tips.SuccessTip;
import com.jfeat.crud.base.tips.Tip;
import com.jfeat.jar.dependency.DependencyUtils;
import com.jfeat.jar.dependency.ZipFileUtils;
import com.jfeat.jar.dependency.model.ChecksumModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping("/jars")
    @ApiOperation(value = "获取配置目录下所有jar文件")
    public Tip getRootJars() {
        String rootPath = jarDeployProperties.getRootPath();
        File rootPathFile = new File(rootPath);
        logger.info("rootPath= {}", rootPathFile.getAbsolutePath());
        Assert.isTrue(rootPathFile.exists(), "jar-deploy:root-path: 配置项不存在！");

        File[] jarFiles = rootPathFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return FilenameUtils.getExtension(s).equals("jar");
            }
        });

        ArrayList<String> filesArray  =new ArrayList<>();
        for(File f : jarFiles){
            filesArray.add(f.getName());
        }

        return SuccessTip.create(filesArray);
    }

    @GetMapping("/jars/{sub}")
    @ApiOperation(value = "获取配置目录下指定目录下的所有jar文件")
    @ApiImplicitParam(name = "sub", value = "查找子目录")
    public Tip getJars(@PathVariable("sub") String subDir) {
        String rootPath = jarDeployProperties.getRootPath();
        File rootPathFile = new File(rootPath);
        Assert.isTrue(rootPathFile.exists(), "jar-deploy:root-path: 配置项不存在！");

        File subPathFile = new File(rootPath + File.separator + subDir);
        logger.info("rootPath= {}", rootPathFile.getAbsolutePath());
        //if(!subPathFile.exists()){
        //    throw  new BusinessException(BusinessCode.BadRequest, "目录不存在: " + subDir);
        //}
        ArrayList<String> filesArray  =new ArrayList<>();

        if(!subPathFile.exists()){
            return SuccessTip.create(filesArray);
        }

        File[] jarFiles = subPathFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return FilenameUtils.getExtension(s).equals("jar");
            }
        });

        for(File f : jarFiles){
            filesArray.add(f.getName());
        }

        return SuccessTip.create(filesArray);
    }


    @PostMapping("/jars/upload/{sub}")
    @ApiOperation(value = "发送.jar至指定目录")
    public Tip uploadJarFile(@RequestPart("file") MultipartFile file, @PathVariable("sub")String subDir) {
        if (file.isEmpty()) {
            throw new BusinessException(BusinessCode.BadRequest, "file is empty");
        }
        Long fileSize = file.getSize();
        if(fileSize==0){
            throw new BusinessException(BusinessCode.BadRequest,  "file is empty");
        }
        /// end sanity

        String rootPath = jarDeployProperties.getRootPath();
        File rootPathFile = new File(rootPath);
        Assert.isTrue(rootPathFile.exists(), "jar-deploy:root-path: 配置项不存在！");

        File subPathFile = new File(rootPath + File.separator + subDir);
        if(!StringUtils.isEmpty(subDir)) {
            if (!subPathFile.exists()) {
                subPathFile.mkdirs();
            }
        }

        String originalFileName = file.getOriginalFilename();
        try {
            File target = new File(subPathFile.getAbsolutePath() + File.separator + originalFileName);
            String path = target.getCanonicalPath();
            boolean readable = target.setReadable(true);
            if(readable){
                logger.info("file uploading to: {}", path);
                FileUtils.copyInputStreamToFile(file.getInputStream(), target);
                logger.info("file uploaded to: {}", target.getAbsolutePath());

                // get relative path
                File appFile = new File("./");
                String relativePatht = target.getAbsolutePath();
                relativePatht = relativePatht.substring(appFile.getAbsolutePath().length()-1, relativePatht.length());
                logger.info("relativePatht={}", relativePatht);

                return SuccessTip.create(relativePatht);

            }else{
                throw new BusinessException(BusinessCode.UploadFileError, "file is not readable");
            }
        } catch (Exception e) {
            throw new BusinessException(BusinessCode.GeneralIOError);
        }
    }


    @GetMapping("/dependencies")
    @ApiOperation(value = "查询jar的依赖")
    public Tip queryJarDependencies(@RequestParam("jar") String jarFileName,
                                    @RequestParam(name="search", required = false)String search) {
        String rootPath = jarDeployProperties.getRootPath();
        File jarFile = new File(rootPath + File.separator + jarFileName);
        if(!jarFile.exists()){
            throw new BusinessException(BusinessCode.BadRequest, jarFileName + " not exists!");
        }

        if(jarFile.setReadable(true)){
            List<String> libDependencies = DependencyUtils.getDependenciesByJar(jarFile);
            if(libDependencies!=null && libDependencies.size()>0) {
                var query= StringUtils.isEmpty(search) ? libDependencies
                        : libDependencies.stream().filter(u->u.contains(search)).collect(Collectors.toList());
                return SuccessTip.create(query);
            }
            return SuccessTip.create(new ArrayList<String>());
        }else{
            throw new BusinessException(BusinessCode.UploadFileError, "file is not readable");
        }
    }
    @GetMapping("/dependencies/{sub}")
    @ApiOperation(value = "查询jar的依赖")
    public Tip queryJarDependencies(@PathVariable("sub") String subDir,
                                    @RequestParam("jar") String jarFileName,
                                    @RequestParam(name="search", required = false) String search) {
        String rootPath = jarDeployProperties.getRootPath();
        File jarFile = new File(rootPath + File.separator + subDir + File.separator + jarFileName);
        if(!jarFile.exists()){
            throw new BusinessException(BusinessCode.BadRequest, jarFileName + " not exists!");
        }
        if(jarFile.setReadable(true)){
            List<String> libDependencies = DependencyUtils.getDependenciesByJar(jarFile);
            if(libDependencies!=null && libDependencies.size()>0) {
                var query= StringUtils.isEmpty(search) ? libDependencies
                        : libDependencies.stream().filter(u->u.contains(search)).collect(Collectors.toList());
                return SuccessTip.create(query);
            }
            return SuccessTip.create(new ArrayList<String>());
        }else{
            throw new BusinessException(BusinessCode.UploadFileError, "file is not readable");
        }
    }

    @GetMapping("/checksum")
    @ApiOperation(value = "查询lib所有依赖的checksum")
    public Tip rootChecksum(@RequestParam("jar") String jarFileName) {
        String rootPath = jarDeployProperties.getRootPath();
        File jarFile = new File(rootPath + File.separator + jarFileName);
        if(!jarFile.exists()){
            throw new BusinessException(BusinessCode.BadRequest, jarFileName + " not exists!");
        }

        if(jarFile.setReadable(true)){
            List<ChecksumModel> libDependencies = DependencyUtils.getChecksumsByJar(jarFile);
            if(libDependencies!=null && libDependencies.size()>0) {
                JSONObject checksums = DependencyUtils.convertChecksumToJSON(libDependencies);
                return SuccessTip.create(checksums);
            }
            return SuccessTip.create(new ArrayList<ChecksumModel>());
        }else{
            throw new BusinessException(BusinessCode.UploadFileError, "file is not readable");
        }
    }

    @GetMapping("/checksum/{sub}")
    @ApiOperation(value = "查询lib所有依赖的checksum")
    public Tip libChecksum(@PathVariable("sub")String subDir, @RequestParam("jar") String jarFileName) {
        String rootPath = jarDeployProperties.getRootPath();

        File target = new File(rootPath + File.separator + subDir + File.separator + jarFileName);
        boolean readable = target.setReadable(true);
        if (readable) {
            List<ChecksumModel> libDependencies = DependencyUtils.getChecksumsByJar(target);
            if(libDependencies!=null && libDependencies.size()>0) {
                JSONObject checksums = DependencyUtils.convertChecksumToJSON(libDependencies);
                return SuccessTip.create(checksums);
            }
            //List<Long> libChecksums = ZipFileUtils.UnzipWithChecksum(target);
            return SuccessTip.create(new ArrayList<ChecksumModel>());
        } else {
            throw new BusinessException(BusinessCode.UploadFileError, "file is not readable");
        }
    }


    private List<String> getMismatchJars(String baseJar, String jar, boolean skipVersion){
        String rootPath = jarDeployProperties.getRootPath();
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

    @GetMapping("/dependency/mismatch")
    @ApiOperation(value = "检查两个JAR的依赖是否匹配")
    public Tip mismatchJars(@RequestParam("baseJar") String baseJar, @RequestParam("jar") String jar) {
        List<String> diffDependencies = getMismatchJars(baseJar, jar, false);
        if (diffDependencies != null && diffDependencies.size() > 0) {
            // mismatch, just delete the file
            //FileUtils.forceDelete(target);
            return SuccessTip.create(diffDependencies);
        }
        return SuccessTip.create(new ArrayList<String>());
    }

    @GetMapping("/dependency/new")
    @ApiOperation(value = "检查两个JAR的依赖是否匹配,忽略版本号比较,找出新依赖")
    public Tip mismatchJarsForNewOnes(@RequestParam("baseJar") String baseJar, @RequestParam("jar") String jar) {
        List<String> diffDependencies = getMismatchJars(baseJar, jar, true);
        if (diffDependencies != null && diffDependencies.size() > 0) {
            // mismatch, just delete the file
            //FileUtils.forceDelete(target);
            return SuccessTip.create(diffDependencies);
        }
        return SuccessTip.create(new ArrayList<String>());
    }

    @GetMapping("/dependency/match")
    @ApiOperation(value = "检查两个JAR的依赖是否匹配")
    public Tip matchJars(@RequestParam("baseJar") String baseJar, @RequestParam("jar") String jar) {
        String rootPath = jarDeployProperties.getRootPath();
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

        // match dependencies
        {
            List<String> appDependencies = DependencyUtils.getDependenciesByJar(rootJarFile);
            List<String> libDependencies = DependencyUtils.getDependenciesByJar(jarFile);

            List<String> diffDependencies = DependencyUtils.getSameDependencies(appDependencies, libDependencies);

            if (diffDependencies != null && diffDependencies.size() > 0) {
                // mismatch, just delete the file
                //FileUtils.forceDelete(target);
                return SuccessTip.create(diffDependencies);
            }
        }
        return SuccessTip.create(new ArrayList<String>());
    }


    @GetMapping("/checksum/mismatch")
    @ApiOperation(value = "依据checksum检查两个jar的更新依赖")
    public Tip checksumMismatchJars(@RequestParam("baseJar") String baseJar, @RequestParam("jar") String jar) {
        String rootPath = jarDeployProperties.getRootPath();
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

        // get mismatch
        List<ChecksumModel> baseJarChecksum = DependencyUtils.getChecksumsByJar(rootJarFile);
        List<ChecksumModel> jarChecksum = DependencyUtils.getChecksumsByJar(jarFile);

        return SuccessTip.create(DependencyUtils.getDifferentChecksums(baseJarChecksum, jarChecksum));
    }


    /**
     * start to deploy the lib jar to standalone jar
     * @param baseJar64  base64Encoded
     * @param jar64  base64Encoded
     * @return
     */
    @PostMapping("/to/{baseJar64}/from/{jar64}")
    @ApiOperation(value = "同步依赖装配lib.jar至应用standalone.jar")
    public Tip mergeJars(@PathVariable("baseJar64") String baseJar64, @PathVariable("jar64") String jar64) {
        String baseJar = new String(Base64.getDecoder().decode(baseJar64));
        String jar = new String(Base64.getDecoder().decode(jar64));

        String rootPath = jarDeployProperties.getRootPath();
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
        File libFile = convertToLibJar(jarFile);
        if(!libFile.setReadable(true)){
            throw new BusinessException(BusinessCode.FileReadingError);
        }
        logger.info("libPath={}", libFile.getAbsolutePath());

        // check dependencies
        List<String> baseJarDependencies = DependencyUtils.getDependenciesByJar(rootJarFile);
        List<String> libDependencies = DependencyUtils.getDependenciesByJar(libFile);
        List<String> diffDependencies = DependencyUtils.getDifferentDependenciesIgnoreVersion(baseJarDependencies, libDependencies);
        if(diffDependencies!=null && diffDependencies.size()>0){
            diffDependencies.forEach(u->logger.debug("dependency= {}",u));
            throw new BusinessException(BusinessCode.Reserved2, "依赖不匹配, 禁止更新jar!");
        }

        // allow inject
        // just wait for cron to deploy the lib
        ZipFileUtils.addFilesToZip(rootJarFile, new File[]{libFile});

        return SuccessTip.create(libFile.getAbsolutePath().replace((new File(rootPath).getAbsolutePath()+File.separator), ""));
    }

    /**
     * deploy
     * @param jarFile
     * @return
     */
    private File convertToLibJar(File jarFile) {
        final String LIB = "BOOT-INF/lib";
        String rootPath = jarDeployProperties.getRootPath();
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
