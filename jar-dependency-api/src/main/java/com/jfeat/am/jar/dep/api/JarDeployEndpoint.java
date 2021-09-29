package com.jfeat.am.jar.dep.api;

import com.google.common.hash.Hashing;
import com.jfeat.am.jar.dep.properties.JarDeployProperties;
import com.jfeat.am.jar.dep.request.JarRequest;
import com.jfeat.am.jar.dep.util.DepUtils;
import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import com.jfeat.crud.base.tips.ErrorTip;
import com.jfeat.crud.base.tips.SuccessTip;
import com.jfeat.crud.base.tips.Tip;
import com.jfeat.jar.dependency.DependencyUtils;
import com.jfeat.jar.dependency.JarUpdate;
import com.jfeat.jar.dependency.ZipFileUtils;
import com.jfeat.jar.dependency.comparable.ChecksumKeyValue;
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

    @PostMapping("/jars/upload/{dir}")
    @ApiOperation(value = "发送.jar至指定目录")
    public Tip uploadJarFile(@RequestPart("file") MultipartFile file,
                             @PathVariable(value = "dir", required = false) String dir) {
        if (file.isEmpty()) {
            throw new BusinessException(BusinessCode.BadRequest, "file is empty");
        }
        Long fileSize = file.getSize();
        if (fileSize == 0) {
            throw new BusinessException(BusinessCode.BadRequest, "file is empty");
        }
        /// end sanity
        if(dir==null) dir="";

        String rootPath = jarDeployProperties.getRootPath();
        File rootPathFile = new File(rootPath);
        Assert.isTrue(rootPathFile.exists(), "jar-deploy:root-path: 配置项不存在！");

        File subPathFile = new File(String.join(File.separator, rootPath, dir));
        if (!StringUtils.isEmpty(dir)) {
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
        if (!jarFile.exists()) {
            throw new BusinessException(BusinessCode.BadRequest, jarFileName + " not exists!");
        }

        // first get jar dependencies
        List<Map.Entry<String,Long>> libDependencies = DependencyUtils.getChecksumsByJar(jarFile);
        if (libDependencies != null && libDependencies.size() > 0) {
            if (!StringUtils.isEmpty(pattern)) {
                libDependencies = libDependencies.stream()
                        .filter(x -> x.getKey().contains(pattern))
                        .collect(Collectors.toList());
            }
            return SuccessTip.create(libDependencies);

        }else if(StringUtils.isNotEmpty(type)) {
            final String[] supportedType = new String[]{"adler32", "crc32", "crc32c", "md5", "sha1", "sha256", "sha512",
                    "adler32l", "crc32l", "crc32cl", "md5l", "sha1l", "sha256l", "sha512l"};
            Assert.isTrue(Stream.of(supportedType).collect(Collectors.toList()).contains(type),
                    "supported type: " + String.join(",", supportedType));

            return SuccessTip.create(Map.entry("checksum", type.endsWith("l") ?
                    DepUtils.getFileChecksumAsLong(jarFile, type) :
                    DepUtils.getFileChecksum(jarFile, type)));
        }

        // default to get file checksum in jar file
        var checksums = ZipFileUtils.listEntriesWithChecksum(jarFile, pattern);
        return SuccessTip.create(checksums.stream()
                .map(c->{
                    return new ChecksumKeyValue<String,Long>(c.getKey(), c.getValue());
                })
                .sorted()
                .collect(Collectors.toList()));
    }


    @GetMapping("/checksum/mismatch")
    @ApiOperation(value = "依据checksum检查两个jar的更新依赖")
    public Tip checksumMismatchJars(@RequestParam("baseJar") String baseJar,
                                    @RequestParam("jar") String jar) throws IOException{
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");

        File baseJarFile = new File(String.join(File.separator, rootPath, baseJar));
        if (!baseJarFile.exists()) {
            throw new BusinessException(BusinessCode.FileNotFound);
        }
        if (!baseJarFile.setReadable(true)) {
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
        // first match jar dependencies
        List<Map.Entry<String,Long>> baseJarChecksum = DependencyUtils.getChecksumsByJar(baseJarFile);
        List<Map.Entry<String,Long>> jarChecksum = DependencyUtils.getChecksumsByJar(jarFile);
        if(baseJarChecksum.size()>0 && jarChecksum.size()>0){
            return SuccessTip.create(DependencyUtils.getDifferentChecksums(baseJarChecksum, jarChecksum));

        }else if(baseJarChecksum.size()==0 && jarChecksum.size()==0){
            // compare files
            List<Map.Entry<String,Long>> baseJarEntryChecksum = ZipFileUtils.listEntriesWithChecksum(baseJarFile, ".class");
            List<Map.Entry<String,Long>> jarEntryChecksum = ZipFileUtils.listEntriesWithChecksum(jarFile, ".class");
            return SuccessTip.create(DependencyUtils.getDifferentChecksums(baseJarEntryChecksum, jarEntryChecksum));

        }else if(baseJarChecksum.size()>0 || jarChecksum.size()>0) {
            File JarFile = jarFile;
            List<Map.Entry<String,Long>> BaseJarChecksum = baseJarChecksum;
            if(jarChecksum.size()>0){
                JarFile = baseJarFile;
                BaseJarChecksum = jarChecksum;
            }

            List<Map.Entry<String,Long>> list = new ArrayList<>();

            String filename =  JarFile.getName();
            var query = BaseJarChecksum.stream()
                    .filter(x-> org.codehaus.plexus.util.FileUtils.filename(x.getKey().replace("/", File.separator)).equals(filename))
                    .collect(Collectors.toList());
            Assert.isTrue(query.size()<=1, "multi match within: " + baseJar);
            String commonKey = query.get(0).getKey();

            var entry = Map.entry(commonKey, DepUtils.getFileChecksumAsLong(JarFile, "adler32"));
            list.add(entry);
            return SuccessTip.create(DependencyUtils.getDifferentChecksums(BaseJarChecksum, list));
        }

        return SuccessTip.create(new ArrayList<>());
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

        var list = ZipFileUtils.listEntriesFromArchive(rootJarFile, pattern);
        return SuccessTip.create(list);
    }

    @PostMapping("/extract")
    @ApiOperation(value = "从.jar中解压匹配文件至指定目录")
    public Tip downloadJarFile(@RequestBody JarRequest request) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");

        var checksums =  DepUtils.extraFilesFromJar(rootPath, request.getDir(), request.getJar(), request.getPattern(), request.getTarget());
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
                return SuccessTip.create(ZipFileUtils.listEntriesFromArchive(jarFile, pattern));
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


    @PostMapping("/deploy")
    @ApiOperation(value = "仅部署")
    public Tip compileJarFile(@RequestBody JarRequest request) throws IOException{
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");
        Assert.isTrue(StringUtils.isNotBlank(request.getJar()), "jar cannot be empty!");

        //the jar may be from another base jar
        //format: app.jar:jar-dependency-1.0.0.jar
        // try to handle the complex jar
        String jar = request.getJar().contains(":") ? request.getJar().substring(request.getJar().indexOf(":")+1) : request.getJar();

        String jarPath = String.join(File.separator, rootPath, request.getTarget(), jar);
        File jarFile = new File(jarPath);
        Assert.isTrue(jarFile.exists() || request.getJar().contains(":"), jar + " not exist !");

        if(!jarFile.exists() && request.getJar().contains(":") ){
            // get the base jar
            String baseJar = request.getJar().substring(0, request.getJar().indexOf(":"));
            File baseJarFile = new File(String.join(File.separator, rootPath, baseJar));
            Assert.isTrue(baseJarFile.exists(), baseJar + " not exists ！");
            // get from base jar
            List<Map.Entry<String,Long>> checksums =  DepUtils.extraFilesFromJar(rootPath, "", baseJar, jar, request.getTarget());
            Assert.isTrue(checksums.size()==1, jar + " must be unique within: " + baseJar);

            jarPath = String.join(File.separator, rootPath, checksums.get(0).getKey());
            jarFile = new File(jarPath);
        }
        
        List<File> classes = new ArrayList<>();

        // dir/javaclass -> classes/javaname.class
        // target to .jar
        if(StringUtils.isNotBlank(request.getPattern())){
            File dirFile = new File(String.join(File.separator, rootPath, request.getDir()));
            Assert.isTrue(dirFile.exists(), request.getDir() + " not exists!");

            File[] listOfFiles = dirFile.listFiles();
            Stream.of(listOfFiles)
                    .filter(f -> FilenameUtils.getExtension(f.getName()).equals("class"))
                    .filter(f -> f.getName().contains(request.getPattern()))
                    .map(
                        f ->{ 
                            return new File(String.join(File.separator, rootPath, request.getDir(), f.getName()));
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
        try {
            // map jar entry names from jar file
            var entryNames =
                    ZipFileUtils.listEntriesFromArchive(jarFile, ".class")
                            .stream()
                            .collect(Collectors.toMap(
                                    entry->org.codehaus.plexus.util.FileUtils.filename(entry.replace("/", File.separator)),
                                    entry->entry));

            // convert filenames to jar entry names
            var entries = classes.stream().map(file -> {
                String filename = org.codehaus.plexus.util.FileUtils.filename(file.getName());
                return entryNames.get(filename);
            }).collect(Collectors.toList());

            List<String> result = JarUpdate.addFiles(jarFile, classes, entries);
            return SuccessTip.create(result);

        }catch (Exception e){
            return ErrorTip.create(BusinessCode.Reserved);
        }
    }


    /**
     * start to deploy the lib jar to standalone jar
     *
     * @param baseJar64 base64Encoded
     * @param jar64     base64Encoded
     * @return
     */
    @Deprecated
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
        File libFile = DepUtils.alignFileJarEntry(jarFile, rootJarFile);
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


    /**
     * 创建索引
     *
     */
    @GetMapping("/indexes")
    @ApiOperation(value = "为.jar创建索引")
    public Tip createJarIndexes(@RequestParam(value = "dir", required = false) String dir,
                                @RequestParam("jar") String jarFileName,
                                @RequestParam(name = "pattern", required = false) String pattern,
                                @RequestParam(name = "target", required = false) String target,
                                @RequestParam(name = "recreate", required = false) Boolean recreate
                                ) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");
        if(dir==null) dir="";
        if(recreate==null) recreate=false;

        String targetPath = target;
        if(StringUtils.isNotBlank(target)){
            var targetDir = new File(String.join(File.separator, rootPath, target));
            org.codehaus.plexus.util.FileUtils.mkdir(targetDir.getAbsolutePath());
            targetPath = targetDir.getAbsolutePath();
        }
        final String finalTargetPath = targetPath;

        File rootJarFile = new File(String.join(File.separator, rootPath, dir, jarFileName));
        Assert.isTrue(rootJarFile.exists(), jarFileName + " not exists !");

        var list = ZipFileUtils.listEntriesFromArchive(rootJarFile, pattern);
        // clean up all indexing files first
        if(recreate){
            list.stream().forEach(entry -> {
                String firstLetter = String.valueOf(org.codehaus.plexus.util.FileUtils.filename(entry.replace("/",File.separator)).charAt(0)).toLowerCase();
                File letterFile = new File(String.join(File.separator, finalTargetPath, firstLetter));
                try {
                    org.codehaus.plexus.util.FileUtils.forceDelete(letterFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        // create indexes files
        String jarFilename = rootJarFile.getName();
        list.stream()
                .filter(f->org.codehaus.plexus.util.FileUtils.extension(f).equals("class"))
                .map(key->{
            Map.Entry<String,String> entry = Map.entry(key, jarFilename);
            return entry;
        }).forEach(entry->{
            String fileName = org.codehaus.plexus.util.FileUtils.filename(entry.getKey().replace("/", File.separator));
            String firstLetter = String.valueOf(fileName.charAt(0)).toLowerCase();
            File letterFile = new File(String.join(File.separator, finalTargetPath, firstLetter));

            try {
                // skip exist ones
                // read content from file
                List<String> lines = letterFile.exists() ? FileUtils.readLines(letterFile, "UTF-8") : new ArrayList<>();
                List<String> contents = lines.stream().map(line->{
                    return line.split(",")[0];
                }).collect(Collectors.toList());

                // append to file
                if(!contents.contains(entry.getKey())) {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(letterFile, true));
                    bw.append(String.join(",", fileName, entry.getValue(), entry.getKey(), "\n"));
                    bw.close();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });

        return SuccessTip.create(list);
    }
}
