package com.jfeat.am.jar.dep.api;

import com.google.common.hash.HashCode;
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
import com.jfeat.jar.dependency.JarUpdate;
import com.jfeat.jar.dependency.ZipFileUtils;
import com.jfeat.jar.dependency.model.JarModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
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
    public Tip uploadJarFile(@RequestPart("file") MultipartFile file, @PathVariable(value = "sub", required = false) String subDir) {
        if (file.isEmpty()) {
            throw new BusinessException(BusinessCode.BadRequest, "file is empty");
        }
        Long fileSize = file.getSize();
        if (fileSize == 0) {
            throw new BusinessException(BusinessCode.BadRequest, "file is empty");
        }
        /// end sanity
        if(subDir==null) subDir="";

        String rootPath = jarDeployProperties.getRootPath();
        File rootPathFile = new File(rootPath);
        Assert.isTrue(rootPathFile.exists(), "jar-deploy:root-path: 配置项不存在！");

        File subPathFile = new File(String.join(File.separator, rootPath, subDir));
        if (!StringUtils.isEmpty(subDir)) {
            if (!subPathFile.exists()) {
                subPathFile.mkdirs();
            }
        }

        String originalFileName = file.getOriginalFilename();
        try {
            File target = new File(String.join(File.separator, subPathFile.getAbsolutePath(), originalFileName));
            String path = target.getCanonicalPath();
            boolean readable = target.setReadable(true);
            if (readable) {
                logger.info("file uploading to: {}", path);
                FileUtils.copyInputStreamToFile(file.getInputStream(), target);
                logger.info("file uploaded to: {}", target.getAbsolutePath());

                // get relative path
                File appFile = new File("./");
                String relativePatht = target.getAbsolutePath();
                relativePatht = relativePatht.substring(appFile.getAbsolutePath().length() - 1, relativePatht.length());
                logger.info("relativePatht={}", relativePatht);

                return SuccessTip.create(relativePatht);

            } else {
                throw new BusinessException(BusinessCode.UploadFileError, "file is not readable");
            }
        } catch (Exception e) {
            throw new BusinessException(BusinessCode.GeneralIOError);
        }
    }

    @GetMapping("/jars")
    @ApiOperation(value = "获取配置目录下指定目录下的所有jar文件")
    @ApiImplicitParam(name = "dir", value = "查找子目录")
    public Tip getJars(@RequestParam(value = "dir", required = false) String dir,
                       @RequestParam(value = "all", required = false) Boolean all
                       ) {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");
        String Dir = dir==null?"":dir;
        Boolean All = all==null? false : all;

        File rootPathFile = new File(rootPath);
        Assert.isTrue(rootPathFile.exists(), "jar-deploy:root-path: 配置项不存在！");

        File subPathFile = new File(String.join(File.separator, rootPath, Dir));
        ArrayList<String> filesArray = new ArrayList<>();

        if (!subPathFile.exists()) {
            return SuccessTip.create(filesArray);
        }

        File[] jarFiles = subPathFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                var ext = FilenameUtils.getExtension(s);
                if(All){
                    return true;
                }
                return ext.equals("jar") || ext.equals("war");
            }
        });

        for (File f : jarFiles) {
            filesArray.add(f.getName());
        }

        return SuccessTip.create(filesArray);
    }


    @GetMapping
    @ApiOperation(value = "查询jar的依赖")
    public Tip queryJarDependencies(@RequestParam("jar") String jarFileName,
                                    @RequestParam(value = "dir", required = false) String dir,
                                    @RequestParam(name = "pattern", required = false) String pattern) {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");
        if(dir==null) dir="";

        File jarFile = new File(String.join(File.separator, rootPath, dir, jarFileName));
        if (!jarFile.exists()) {
            throw new BusinessException(BusinessCode.BadRequest, jarFileName + " not exists!");
        }

        if (jarFile.setReadable(true)) {
            List<String> libDependencies = DependencyUtils.getDependenciesByJar(jarFile);
            if (libDependencies != null && libDependencies.size() > 0) {
                var query = StringUtils.isEmpty(pattern) ? libDependencies
                        : libDependencies.stream().filter(u -> u.contains(pattern)).collect(Collectors.toList());
                return SuccessTip.create(query);
            }
            return SuccessTip.create(new ArrayList<String>());
        } else {
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
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");

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
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");

        if (major) {
            return SuccessTip.create(DepUtils.getMatchJars(rootPath, baseJar, jar, true));
        }
        return SuccessTip.create(DepUtils.getMatchJars(rootPath, baseJar, jar));
    }


    @GetMapping("/checksum")
    @ApiOperation(value = "查询lib所有依赖的checksum")
    public Tip rootChecksum(@RequestParam(value = "dir", required = false) String dir,
                            @RequestParam("jar") String jarFileName,
                            @RequestParam(value = "type", required = false) String type,
                            @RequestParam(name = "pattern", required = false) String pattern) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");
        if(dir==null){ dir=""; }

        File jarFile = new File(String.join(File.separator, rootPath, dir, jarFileName));

        // if type is not empty, just get the file checksum
        if(StringUtils.isEmpty(type)) {
            if (!jarFile.exists()) {
                throw new BusinessException(BusinessCode.BadRequest, jarFileName + " not exists!");
            }

            List<JarModel> libDependencies = DependencyUtils.getChecksumsByJar(jarFile);
            if (libDependencies != null && libDependencies.size() > 0) {
                if (!StringUtils.isEmpty(pattern)) {
                    libDependencies = libDependencies.stream()
                            .filter(x -> x.getJar().contains(pattern))
                            .collect(Collectors.toList());
                }
                return SuccessTip.create(libDependencies);
            }
        }

        HashCode checksumCode = Files.hash(jarFile, Hashing.md5());
        if(StringUtils.isNotEmpty(type)) {
            final String[] supportedType  =new String[]{"adler32","crc32","crc32c","md5","sha1","sha256","sha512",
                    "adler32l","crc32l","crc32cl","md5l","sha1l","sha256l","sha512l"};
            Assert.isTrue(Stream.of(supportedType).collect(Collectors.toList()).contains(type),
                    "supported type: " + String.join(",", supportedType));
            switch (type) {
                case "adler32":
                case "adler32l":
                    checksumCode = Files.hash(jarFile, Hashing.adler32());
                    break;
                case "crc32":
                case "crc32l":
                    checksumCode = Files.hash(jarFile, Hashing.crc32());
                    break;
                case "crc32c":
                case "crc32cl":
                    checksumCode = Files.hash(jarFile, Hashing.crc32c());
                    break;
                case "md5":
                case "md5l":
                    checksumCode = Files.hash(jarFile, Hashing.md5());
                    break;
                case "sha1":
                case "sha1l":
                    checksumCode = Files.hash(jarFile, Hashing.sha1());
                    break;
                case "sha256":
                case "sha256l":
                    checksumCode = Files.hash(jarFile, Hashing.sha256());
                    break;
                case "sha512":
                case "sha512l":
                    checksumCode = Files.hash(jarFile, Hashing.sha512());
                    break;
                default:
                    break;
            }
        }else{
            type = "adler32l";
        }

        return SuccessTip.create(Map.entry("checksum", type.endsWith("l")?checksumCode.padToLong():checksumCode.toString()));
    }

    @GetMapping("/checksum/mismatch")
    @ApiOperation(value = "依据checksum检查两个jar的更新依赖")
    public Tip checksumMismatchJars(@RequestParam("baseJar") String baseJar, @RequestParam("jar") String jar) {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");

        File rootJarFile = new File(String.join(File.separator, rootPath, baseJar));
        if (!rootJarFile.exists()) {
            throw new BusinessException(BusinessCode.FileNotFound);
        }
        if (!rootJarFile.setReadable(true)) {
            throw new BusinessException(BusinessCode.FileReadingError);
        }
        File jarFile = new File(String.join(File.separator, rootPath, jar));
        if (!jarFile.exists()) {
            throw new BusinessException(BusinessCode.FileNotFound);
        }
        if (!jarFile.setReadable(true)) {
            throw new BusinessException(BusinessCode.FileReadingError);
        }

        // get mismatch
        List<JarModel> baseJarChecksum = DependencyUtils.getChecksumsByJar(rootJarFile);
        List<JarModel> jarChecksum = DependencyUtils.getChecksumsByJar(jarFile);

        return SuccessTip.create(DependencyUtils.getDifferentChecksums(baseJarChecksum, jarChecksum));
    }

    /// deploy
    @GetMapping("/inspect")
    @ApiOperation(value = "从.jar中匹配查找文件")
    public Tip queryJarFile(@RequestParam(value = "dir", required = false) String dir,
                            @RequestParam("jar") String jarFileName,
                            @RequestParam(name = "pattern", required = false) String pattern) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");
        if(dir==null) dir="";

        File rootJarFile = new File(String.join(File.separator, rootPath, dir, jarFileName));
        Assert.isTrue(rootJarFile.exists(), jarFileName + " not exists !");

        var list = ZipFileUtils.listFilesFromArchive(rootJarFile, pattern);
        return SuccessTip.create(list);
    }

    @PostMapping("/extract")
    @ApiOperation(value = "从.jar中解压匹配文件至指定目录")
    public Tip downloadJarFile(@RequestBody JarRequest request) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");

        File rootJarFile = new File(String.join(File.separator, rootPath, request.getDir(), request.getJar()));

        if(StringUtils.isNotEmpty(request.getTarget())){
            String targetPath = String.join(File.separator, rootPath, request.getTarget());
            if(!new File(targetPath).exists()){
                org.codehaus.plexus.util.FileUtils.mkdir(targetPath);
            }
        }

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
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");

        final String Dir =  dir==null?"":dir;

        List<String> files = null;

        // javaclass -> p1
        File javaclasFile = new File(String.join(File.separator, rootPath, Dir, javaclass));
        if (javaclasFile.exists()) {
            List<String> fileList = Stream.of(new String[]{javaclass}).collect(Collectors.toList());
            files = fileList.stream().map(
                    f -> String.join(File.separator, rootPath, Dir, f)
            ).collect(Collectors.toList());

        } else if (org.apache.commons.lang3.StringUtils.isNotBlank(jar)) {
            // jar -> p2
            //Assert.isTrue(org.apache.commons.lang3.StringUtils.isNotBlank(pattern), "pattern should be empty when decompile classes from jar !");

            File jarFile = new File(String.join(File.separator, rootPath, Dir, jar));
            Assert.isTrue(jarFile.exists(), jar + " not exists!");
            if (org.apache.commons.lang3.StringUtils.isBlank(pattern)) {
                return SuccessTip.create(ZipFileUtils.listFilesFromArchive(jarFile, pattern));
            }

            var unzipFiles = ZipFileUtils.unzipFilesFromArchiva(jarFile, pattern, target);
            files = unzipFiles.stream()
                    .filter(f -> FilenameUtils.getExtension(f).equals("class"))
                    .map(
                            f -> String.join(File.separator, rootPath, Dir, f)
                    )
                    .collect(Collectors.toList());

        } else {
            // dir -> p3

            File dirFile = new File(String.join(File.separator, rootPath, Dir));
            Assert.isTrue(dirFile.exists(), jar + " not exists!");

            File[] listOfFiles = dirFile.listFiles();
            files = Stream.of(listOfFiles)
                    .filter(f -> FilenameUtils.getExtension(f.getName()).equals("class"))
                    .filter(f -> f.getName().contains(pattern))
                    .map(
                            f -> String.join(File.separator, rootPath, Dir, f.getName())
                    )
                    .collect(Collectors.toList());
        }

        // decompile
        final StringBuilder decompiles = new StringBuilder();
        OutputSinkFactory.Sink println = line -> {
            decompiles.append(line);
            System.out.println(line);
        };

        OutputSinkFactory mySink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                // I only understand how to sink strings, regardless of what you have to give me.
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                return sinkType == SinkType.JAVA ? println : ignore -> {
                };
            }
        };

        CfrDriver cfrDriver = new CfrDriver.Builder().withOutputSink(mySink).build();
        //CfrDriver cfrDriver = (new CfrDriver.Builder()).build();
        cfrDriver.analyse(files);

        if (!(empty == null || !empty)) {
            files.stream().forEach(
                    filePath -> {
                        org.codehaus.plexus.util.FileUtils.fileDelete(filePath);
                        try {
                            String dirname = org.codehaus.plexus.util.FileUtils.dirname(filePath);
                            File dirFile = new File(dirname);
                            if (dirFile.listFiles().length == 0) {
                                org.codehaus.plexus.util.FileUtils.forceDelete(dirFile);
                            }
                        }catch (IOException e){
                        }
                    }
            );
        }
        return SuccessTip.create(decompiles);
    }


    @PostMapping("/javaclass/deploy")
    @ApiOperation(value = "仅部署")
    public Tip compileJarFile(@RequestBody JarRequest request) throws IOException{
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");

        Assert.isTrue(StringUtils.isNotBlank(request.getJar()), "jar cannot be empty!");
        String jarPath = String.join(File.separator, rootPath, request.getDir(), request.getJar());
        File jarFile = new File(jarPath);
        Assert.isTrue(jarFile.exists(), request.getJar() + " not exist !");

        List<String> results = new ArrayList<>();

        if(StringUtils.isNotEmpty(request.getJavaclass())){
            // deploy to jar
            String classPath = String.join(File.separator, rootPath, request.getDir(), request.getJavaclass());
            File classFile = new File(classPath);
            Assert.isTrue(classFile.exists(), classPath + " not exists!");

            File okClassFile = DepUtils.alignJarEntry(jarFile, classFile);

            // update into zip/jar
            //String result = ZipFileUtils.addFileToZip(jarFile, okClassFile);
            //long crc32=Files.hash(okClassFile, Hashing.adler32()).padToLong();
            String result = JarUpdate.addFile(jarFile, okClassFile);
            results.add(result);
        }

        return SuccessTip.create(results);
    }


    /**
     * start to deploy the lib jar to standalone jar
     *
     * @param baseJar64 base64Encoded
     * @param jar64     base64Encoded
     * @return
     */
    @PostMapping("/deploy/{baseJar64}/from/{jar64}")
    @ApiOperation(value = "同步依赖装配lib.jar至应用standalone.jar")
    public Tip mergeJars(@PathVariable("baseJar64") String baseJar64, @PathVariable("jar64") String jar64) throws IOException {
        String baseJar = new String(Base64.getDecoder().decode(baseJar64));
        String jar = new String(Base64.getDecoder().decode(jar64));

        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");

        File rootJarFile = new File(String.join(File.separator, rootPath, baseJar));
        if (!rootJarFile.exists()) {
            throw new BusinessException(BusinessCode.FileNotFound);
        }
        if (!rootJarFile.setReadable(true)) {
            throw new BusinessException(BusinessCode.FileReadingError);
        }
        File jarFile = new File(String.join(File.separator, rootPath, jar));
        if (!jarFile.exists()) {
            throw new BusinessException(BusinessCode.FileNotFound);
        }
        File libFile = DepUtils.alignJarEntry(jarFile, rootJarFile);
        if (!libFile.setReadable(true)) {
            throw new BusinessException(BusinessCode.FileReadingError);
        }
        logger.info("libPath={}", libFile.getAbsolutePath());

        // check dependencies
        List<String> baseJarDependencies = DependencyUtils.getDependenciesByJar(rootJarFile);
        List<String> libDependencies = DependencyUtils.getDependenciesByJar(libFile);
        List<String> diffDependencies = DependencyUtils.getDifferentDependenciesIgnoreVersion(baseJarDependencies, libDependencies);

        if (diffDependencies != null && diffDependencies.size() > 0) {
            diffDependencies.forEach(u -> logger.debug("dependency= {}", u));
            throw new BusinessException(BusinessCode.Reserved2, "依赖不匹配, 禁止更新jar!");
        }

        // allow inject
        // just wait for cron to deploy the lib
        ZipFileUtils.addFilesToZip(rootJarFile, new File[]{libFile});

        return SuccessTip.create(libFile.getAbsolutePath().replace((new File(rootPath).getAbsolutePath() + File.separator), ""));
    }
}
