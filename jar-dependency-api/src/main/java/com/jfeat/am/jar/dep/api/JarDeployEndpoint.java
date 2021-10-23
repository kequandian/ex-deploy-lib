package com.jfeat.am.jar.dep.api;

import com.jfeat.am.jar.dep.properties.JarDeployProperties;
import com.jfeat.am.jar.dep.request.JarRequest;
import com.jfeat.am.jar.dep.util.DecompileUtils;
import com.jfeat.am.jar.dep.util.DepUtils;
import com.jfeat.am.jar.dep.util.UploadUtils;
import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import com.jfeat.crud.base.tips.SuccessTip;
import com.jfeat.crud.base.tips.Tip;
import com.jfeat.jar.dependency.DependencyUtils;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * 依赖处理接口
 *
 * @author zxchengb
 * @date 2020-08-05
 */
@RestController
@Api("api-jar-dep")
@RequestMapping("/api/jar/dep")
public class JarDeployEndpoint {
    protected final static Logger logger = LoggerFactory.getLogger(JarDeployEndpoint.class);

    @Autowired
    private JarDeployProperties jarDeployProperties;

    @PostMapping("/jars/upload/{dir}")
    @ApiOperation(value = "发送.jar至指定目(支持base64格式)")
    public Tip uploadJarFile(@RequestPart("file") MultipartFile file,
                             @PathVariable(value = "dir", required = false) String dir) throws IOException {
        if(dir==null) dir="";
        String rootPath = jarDeployProperties.getRootPath();
        File rootPathFile = new File(rootPath);
        Assert.isTrue(rootPathFile.exists(), "jar-dependency:root-path: 配置项不存在！");

        File uploadedFile = null;
        if(file!=null && !file.isEmpty()) {
            uploadedFile = UploadUtils.doMultipartFile(file, String.join(File.separator, rootPath, dir));
        }

        // get relative path
        File appFile = new File("./");
        String relativePath = uploadedFile.getAbsolutePath();
        relativePath = relativePath.substring(appFile.getAbsolutePath().length() - 1, relativePath.length());
        logger.info("relative path={}", relativePath);

        return SuccessTip.create(relativePath);
    }

    @PostMapping("/jars/upload64/{dir}")
    @ApiOperation(value = "发送.jar至指定目(支持base64格式)")
    public Tip uploadJarFileBase64(@PathVariable(value = "dir", required = false) String dir,
                             HttpServletRequest request) throws IOException {
        if(dir==null) dir="";
        String rootPath = jarDeployProperties.getRootPath();
        File rootPathFile = new File(rootPath);
        Assert.isTrue(rootPathFile.exists(), "jar-dependency:root-path: 配置项不存在！");

        File uploadedFile = UploadUtils.doBase64File(request, String.join(File.separator, rootPath, dir));

        // get relative path
        File appFile = new File("./");
        String relativePath = uploadedFile.getAbsolutePath();
        relativePath = relativePath.substring(appFile.getAbsolutePath().length() - 1, relativePath.length());
        logger.info("relative path={}", relativePath);

        return SuccessTip.create(relativePath);
    }


    @GetMapping("/jars")
    @ApiOperation(value = "获取配置目录下指定目录下的所有jar文件")
    @ApiImplicitParam(name = "dir", value = "查找子目录")
    public Tip getJars(@RequestParam(value = "dir", required = false) String dir,
                       @RequestParam(value = "all", required = false) Boolean all
                       ) {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");
        String Dir = dir==null?"":dir;
        Boolean All = all==null? false : all;

        File rootPathFile = new File(rootPath);
        Assert.isTrue(rootPathFile.exists(), "jar-dependency:root-path: 配置项不存在！");

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
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");
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
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");

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
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");

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
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");
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
                    ZipFileUtils.getFileChecksumCode(jarFile, type).padToLong() :
                    ZipFileUtils.getFileChecksum(jarFile, type)));
        }

        // default to get file checksum in jar file
        var checksums = ZipFileUtils.listEntriesWithChecksum(jarFile, "jar", pattern);
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
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");

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
            List<Map.Entry<String,Long>> baseJarEntryChecksum = ZipFileUtils.listEntriesWithChecksum(baseJarFile, "class", "");
            List<Map.Entry<String,Long>> jarEntryChecksum = ZipFileUtils.listEntriesWithChecksum(jarFile, "class", "");
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

            var entry = Map.entry(commonKey, ZipFileUtils.getFileChecksumCode(JarFile, "adler32").padToLong());
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
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");
        if(dir==null) dir="";

        File rootJarFile = new File(String.join(File.separator, rootPath, dir, jarFileName));
        Assert.isTrue(rootJarFile.exists(), jarFileName + " not exists !");

        var list = ZipFileUtils.listEntriesFromArchive(rootJarFile, "", pattern);
        return SuccessTip.create(list);
    }

    @PostMapping("/extract")
    @ApiOperation(value = "从.jar中解压匹配文件至指定目录")
    public Tip downloadJarFile(@RequestBody JarRequest request) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");

        // convert to target
        String absTarget = String.join(File.separator, rootPath, request.getTarget());
        File targetFile = new File(absTarget);

        File jarFile = new File(String.join(File.separator, rootPath, request.getDir(), request.getJar()));
        Assert.isTrue(jarFile.exists(), jarFile.getAbsolutePath()+" no exists !");

        var checksums =  DepUtils.extraFilesFromJar(jarFile, "", request.getPattern(), targetFile);
        return SuccessTip.create(checksums);
    }

    @GetMapping("/decompile")
    @ApiOperation(value = "反编译指定的文件")
    public Tip decompileJarFile(@RequestParam(value = "dir", required = false) String dir,
                                @RequestParam(value = "pattern", required = false) String pattern,
                                @RequestParam(value = "jar", required = false) String jar,
                                @RequestParam(value = "target", required = false) String target
    ) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");
        final String Dir =  dir==null?"":dir;

        List<String> files = null;

        if (org.apache.commons.lang3.StringUtils.isNotBlank(jar)) {
            // jar -> p1

            File jarFile = new File(String.join(File.separator, rootPath, Dir, jar));
            Assert.isTrue(jarFile.exists(), jar + " not exists!");
            if (org.apache.commons.lang3.StringUtils.isBlank(pattern)) {
                return SuccessTip.create(ZipFileUtils.listEntriesFromArchive(jarFile, "", pattern));
            }

            File targetPath = new File(String.join(File.separator, rootPath, target));
            var unzipFiles = ZipFileUtils.unzipFilesFromArchiva(jarFile, "", pattern, targetPath);
            files = unzipFiles.stream()
                    .filter(f -> FilenameUtils.getExtension(f).equals("class"))
                    .map(
                            f -> String.join(File.separator, rootPath, Dir, f)
                    )
                    .collect(Collectors.toList());

        } else {
            // dir -> p2

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

        // start to decompile
        List<String> decompiles = DecompileUtils.decompileFiles(files, true);
        return SuccessTip.create(decompiles.size());
    }


    @PostMapping("/deploy/{type}")
    @ApiOperation(value = "部署目录中指定type类型(如 .class)的文件")
    public Tip deployClasses(@RequestBody JarRequest request,
                             @PathVariable("type") String fileExtension) throws IOException{
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");

        String jarPath = String.join(File.separator, rootPath, request.getTarget(),  request.getJar());
        File jarFile = new File(jarPath);
        Assert.isTrue(jarFile.exists(), jarFile.getAbsolutePath() + " not exists!");

        File dirPath = new File(String.join(File.separator, rootPath, request.getDir()));
        Assert.isTrue(dirPath.exists(), dirPath.getAbsolutePath() + " not exists!");

        var deployedList  =DepUtils.deployFilesToJar(dirPath, fileExtension, request.getPattern(), jarFile);
        return SuccessTip.create(deployedList);
    }


    @PostMapping("/deploy")
    @ApiOperation(value = "仅部署")
    public Tip compileJarFile(@RequestBody JarRequest request) throws IOException{
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");

        File dirPath = new File(String.join(File.separator, rootPath, request.getDir()));
        Assert.isTrue(dirPath.exists(), dirPath.getAbsolutePath() + " not exists!");

        File jarFile = new File(String.join(File.separator, rootPath, request.getTarget(), request.getJar()));

        var deployedList  =DepUtils.deployFilesToJarEntry(dirPath, "class", request.getPattern(),
                jarFile, request.getEntry());

        return SuccessTip.create(deployedList);
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
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");
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

        var list = ZipFileUtils.listEntriesFromArchive(rootJarFile, "class", pattern);
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
                //.filter(f->org.codehaus.plexus.util.FileUtils.extension(f).equals("class"))
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
