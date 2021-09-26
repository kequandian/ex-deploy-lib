package com.jfeat.am.jar.dep.api;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.jfeat.am.jar.dep.properties.JarDeployProperties;
import com.jfeat.am.jar.dep.request.JarRequest;
import com.jfeat.am.jar.dep.util.DepUtils;
import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import com.jfeat.crud.base.tips.SuccessTip;
import com.jfeat.crud.base.tips.Tip;
import com.jfeat.jar.dependency.DependencyUtils;
import com.jfeat.jar.dependency.ZipFileUtils;
import com.jfeat.jar.dependency.model.JarModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 依赖处理接口
 *
 * @author zxchengb
 * @date 2020-08-05
 */
@RestController
@Api("/api/jar/dep")
@RequestMapping("/api/jar/dep")
public class JarDeployEndpoint {
    protected final static Logger logger = LoggerFactory.getLogger(JarDeployEndpoint.class);

    @Autowired
    private JarDeployProperties jarDeployProperties;

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

    @GetMapping("/jars")
    @ApiOperation(value = "获取配置目录下指定目录下的所有jar文件")
    @ApiImplicitParam(name = "sub", value = "查找子目录")
    public Tip getJars(@RequestParam(value = "dir", required = false) String dir) {

        String rootPath = jarDeployProperties.getRootPath();
        File rootPathFile = new File(rootPath);
        Assert.isTrue(rootPathFile.exists(), "jar-deploy:root-path: 配置项不存在！");

        File subPathFile = new File(String.join(File.separator, rootPath, dir));
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

    @GetMapping
    @ApiOperation(value = "查询jar的依赖")
    public Tip queryJarDependencies(@RequestParam("jar") String jarFileName,
                                    @RequestParam(value = "dir",required = false) String dir,
                                    @RequestParam(name="pattern", required = false) String pattern) {
        String rootPath = jarDeployProperties.getRootPath();
        File jarFile = new File(rootPath + File.separator + jarFileName);
        if(!jarFile.exists()){
            throw new BusinessException(BusinessCode.BadRequest, jarFileName + " not exists!");
        }

        if(jarFile.setReadable(true)){
            List<String> libDependencies = DependencyUtils.getDependenciesByJar(jarFile);
            if(libDependencies!=null && libDependencies.size()>0) {
                var query= StringUtils.isEmpty(pattern) ? libDependencies
                        : libDependencies.stream().filter(u->u.contains(pattern)).collect(Collectors.toList());
                return SuccessTip.create(query);
            }
            return SuccessTip.create(new ArrayList<String>());
        }else{
            throw new BusinessException(BusinessCode.UploadFileError, "file is not readable");
        }
    }

    @GetMapping("/mismatch")
    @ApiOperation(value = "检查两个JAR的依赖是否匹配")
    public Tip mismatchJars(@RequestParam("baseJar") String baseJar,
                            @RequestParam("jar") String jar,
                            @ApiParam(name = "major", value = "仅匹配库名,不匹配版本号")
                            @RequestParam(value = "major", required = false) boolean major
    ) {
        String rootPath = jarDeployProperties.getRootPath();
        List<String> diffDependencies = DepUtils.getMismatchJars(rootPath, baseJar, jar, major);
        if (diffDependencies != null && diffDependencies.size() > 0) {
            return SuccessTip.create(diffDependencies);
        }
        return SuccessTip.create(new ArrayList<String>());
    }

    @GetMapping("/match")
    @ApiOperation(value = "检查两个JAR的依赖是否匹配")
    public Tip matchJars(@RequestParam("baseJar") String baseJar,
                         @RequestParam("jar") String jar,
                         @ApiParam(name = "major", value = "仅匹配库名,不匹配版本号")
                             @RequestParam(value = "major", required = false) boolean major) {
        String rootPath = jarDeployProperties.getRootPath();
        if(major){
            return SuccessTip.create(DepUtils.getMatchJars(rootPath, baseJar, jar, true));
        }
        return SuccessTip.create(DepUtils.getMatchJars(rootPath, baseJar, jar));
    }



@GetMapping("/checksum")
@ApiOperation(value = "查询lib所有依赖的checksum")
public Tip rootChecksum(@RequestParam(value = "dir", required = false)String dir,
                        @RequestParam("jar") String jarFileName,
                        @RequestParam(name="pattern", required = false) String pattern) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();

        File jarFile = new File(rootPath + File.separator + jarFileName);
        if(!jarFile.exists()){
            throw new BusinessException(BusinessCode.BadRequest, jarFileName + " not exists!");
        }

        List<JarModel> libDependencies = DependencyUtils.getChecksumsByJar(jarFile);
        if(libDependencies!=null && libDependencies.size()>0) {
            if(!StringUtils.isEmpty(pattern)){
                libDependencies = libDependencies.stream()
                        .filter(x->x.getJar().contains(pattern))
                        .collect(Collectors.toList());
            }

            return SuccessTip.create(libDependencies);
        }

        //HashCode md5 = Files.hash(jarFile, Hashing.md5());
        return SuccessTip.create(Map.entry("checksum", Files.hash(jarFile, Hashing.crc32()).padToLong()));
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
        List<JarModel> baseJarChecksum = DependencyUtils.getChecksumsByJar(rootJarFile);
        List<JarModel> jarChecksum = DependencyUtils.getChecksumsByJar(jarFile);

        return SuccessTip.create(DependencyUtils.getDifferentChecksums(baseJarChecksum, jarChecksum));
    }


    /// deploy
    @GetMapping("/list")
    @ApiOperation(value = "从.jar中匹配查找文件")
    public Tip queryJarFile(@RequestParam("dir") String dir,
                            @RequestParam("jar") String jarFileName,
                            @RequestParam(name="pattern", required = false) String pattern) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        File rootJarFile = new File(String.join(File.separator, rootPath, dir, jarFileName));

        var list = ZipFileUtils.listFilesFromArchive(rootJarFile, pattern);
        return SuccessTip.create(list);
    }

    @PostMapping("/extract")
    @ApiOperation(value = "从.jar中解压匹配文件至指定目录")
    public Tip downloadJarFile(@RequestBody JarRequest request) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        File rootJarFile = new File(String.join(File.separator, rootPath, request.getDir(), request.getJar()));

        var checksums = ZipFileUtils.UnzipWithChecksum(rootJarFile, request.getPattern(), request.getTarget());
        return SuccessTip.create(checksums);
    }

    @GetMapping("/decompile")
    @ApiOperation(value = "反编译指定的文件")
    public Tip decompileJarFile(@RequestParam(value = "dir", required = false) String dir,
                                @RequestParam(value = "jar", required = false) String jar,
                                @RequestParam(value = "target", required = false) String target,
                                @RequestParam(value = "pattern", required = false) String pattern,
                                @RequestParam(value = "javaclass", required = false) String javaclass,
                                @RequestParam(value = "empty", required = false) Boolean empty
                                ) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();

        List<String> files = null;

        // javaclass -> p1
        File javaclasFile = new File(String.join(File.separator, rootPath, dir, javaclass));
        if(javaclasFile.exists()) {
            List<String> fileList = Stream.of(new String[]{javaclass}).collect(Collectors.toList());
            files = fileList.stream().map(
                    f -> String.join(File.separator, rootPath, dir, f)
            ).collect(Collectors.toList());

        }else if(org.apache.commons.lang3.StringUtils.isNotBlank(jar)) {
            // jar -> p2
            //Assert.isTrue(org.apache.commons.lang3.StringUtils.isNotBlank(pattern), "pattern should be empty when decompile classes from jar !");

            File jarFile = new File(String.join(File.separator, rootPath, dir, jar));
            Assert.isTrue(jarFile.exists(), jar + " not exists!");
            if(org.apache.commons.lang3.StringUtils.isBlank(pattern)) {
                return SuccessTip.create(ZipFileUtils.listFilesFromArchive(jarFile, pattern));
            }

            var unzipFiles = ZipFileUtils.unzipFilesFromArchiva(jarFile, pattern, target);
            files = unzipFiles.stream()
                    .filter(f->FilenameUtils.getExtension(f).equals("class"))
                    .map(
                            f -> String.join(File.separator, rootPath, dir, f)
                    )
                    .collect(Collectors.toList());

        }else {
            // dir -> p3

            File dirFile = new File(String.join(File.separator, rootPath, dir));
            Assert.isTrue(dirFile.exists(), jar + " not exists!");

            File[] listOfFiles = dirFile.listFiles();
            files = Stream.of(listOfFiles)
                    .filter(f->FilenameUtils.getExtension(f.getName()).equals("class"))
                    .filter(f->f.getName().contains(pattern))
                    .map(
                            f -> String.join(File.separator, rootPath, dir, f.getName())
                    )
                    .collect(Collectors.toList());
        }

        // decompile
        final StringBuilder decompiles = new StringBuilder();
        OutputSinkFactory mySink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                // I only understand how to sink strings, regardless of what you have to give me.
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                return sinkType == SinkType.JAVA ? line->decompiles.append(line) : ignore -> {};
            }
        };

        CfrDriver cfrDriver = new CfrDriver.Builder().withOutputSink(mySink).build();
        //CfrDriver cfrDriver = (new CfrDriver.Builder()).build();
        cfrDriver.analyse(files);

        if(empty){
            files.stream().forEach(
                    filePath->org.codehaus.plexus.util.FileUtils.fileDelete(filePath)
            );
        }

        return SuccessTip.create(decompiles);
    }



    @PostMapping("/deploy")
    @ApiOperation(value = "反编译指定的文件")
    public Tip compileJarFile(@RequestBody JarRequest request) {
        String rootPath = jarDeployProperties.getRootPath();

        //ZipFileUtils.UnzipWithChecksum();

        return SuccessTip.create(new ArrayList<String>());
    }

    /**
     * start to deploy the lib jar to standalone jar
     * @param baseJar64  base64Encoded
     * @param jar64  base64Encoded
     * @return
     */
    @PostMapping("/deploy/{baseJar64}/from/{jar64}")
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
        File libFile = DepUtils.convertToLibJar(jarFile, rootPath);
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
}
