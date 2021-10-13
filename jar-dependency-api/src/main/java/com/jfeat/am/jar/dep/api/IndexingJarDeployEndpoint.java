package com.jfeat.am.jar.dep.api;

import com.jfeat.am.jar.dep.properties.JarDeployProperties;
import com.jfeat.am.jar.dep.util.DecompileUtils;
import com.jfeat.am.jar.dep.util.IndexingUtils;
import com.jfeat.am.jar.dep.util.UploadUtils;
import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.tips.ErrorTip;
import com.jfeat.crud.base.tips.SuccessTip;
import com.jfeat.crud.base.tips.Tip;
import com.jfeat.jar.dependency.ZipFileUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 依赖处理接口
 *
 * @author zxchengb
 * @date 2020-08-05
 */
@RestController
@Api("api-jar-dep-sugar")
@RequestMapping("/api/jar/dep/sugar")
public class IndexingJarDeployEndpoint {
    protected final static Logger logger = LoggerFactory.getLogger(IndexingJarDeployEndpoint.class);

    @Autowired
    private JarDeployProperties jarDeployProperties;

    @PostMapping("/restart")
    @ApiOperation(value = "重启容器")
    public Tip restartContainer() throws IOException {
        String endpoint = jarDeployProperties.getDockerApiEndpoint();
        Assert.isTrue(StringUtils.isNotBlank(endpoint), "jar-dependency:docker-api-endpoint: 没有配置！");
        String container = jarDeployProperties.getContainer();
        Assert.isTrue(StringUtils.isNotBlank(container), "jar-dependency:container: 没有配置！");

        // send restart event
        RestTemplate restTemplate = new RestTemplate();

        final String api = String.join("/", "/containers", container, "restart");
        final String baseUrl = endpoint + api;
        URI uri = null;
        try {
            uri = new URI(baseUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
//            MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
//            map.add("email", "first.last@example.com");
//            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            HttpEntity<Object> request = new HttpEntity<>(headers);

            ResponseEntity<Object> result = restTemplate.postForEntity(uri, request, Object.class);
//            Object result = restTemplate.postForObject(uri, request, Object.class);
            return SuccessTip.create(result);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return ErrorTip.create(BusinessCode.Reserved);
    }

    @GetMapping("/indexing")
    @ApiOperation(value = "创建索引用于自动部署.class/.jar")
    public Tip indexingJarFile(@RequestParam(value = "jar") String fatJar,
                               @ApiParam("在fat jar中匹配要创建其Entry索引的.jar文件")
                               @RequestParam(value = "pattern", required = false) String pattern,
                               @ApiParam("reset:清空, recreate: 重新构建, append: 增加")
                               @RequestParam(value = "action", required = false) String action)
    throws IOException{
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");
        String defaultLibPath  = "lib";
        String defaultIndexesPath = "indexes";

        final String RESET="reset", RECREATE="recreate", APPEND="append";
        Assert.isTrue(StringUtils.isBlank(action) || Stream.of(new String[]{RESET,RECREATE,APPEND}).collect(Collectors.toList()).contains(action),
                "action should be one of [reset, recreate, append]");

        File fatJarFile = new File(String.join(File.separator, rootPath, fatJar));
        Assert.isTrue(fatJarFile.exists(), fatJarFile.getName() + " not exists !");
        File targetIndexesPath = new File(String.join(File.separator, rootPath, defaultIndexesPath));

        // 如果pattern为空，仅显示文件列表, 不进行索引
        if(StringUtils.isBlank(pattern)) {
            SuccessTip.create(ZipFileUtils.listEntriesFromArchive(fatJarFile, "", ""));
        }

        /// start to indexing
        List<String> allIndexes = new ArrayList<>();

        if(RESET.equals(action)){
            targetIndexesPath.deleteOnExit();
        }else if(RECREATE.equals(action)){
            targetIndexesPath.deleteOnExit();
            targetIndexesPath.mkdir();

            // 1. indexing all files from fat jarFile first
            List<String> fatJarIndexes = IndexingUtils.indexingJarFile(fatJarFile, "", "", targetIndexesPath, false);
            allIndexes.addAll(fatJarIndexes);
        }

        if(RECREATE.equals(action)||APPEND.equals(action)) {

            // 2. indexing jar pattern within fat jar
            // get jar entries first
            File defaultLibPathFile = new File(String.join(File.separator, rootPath, defaultLibPath));
            if (!defaultLibPathFile.exists()) {
                org.codehaus.plexus.util.FileUtils.mkdir(defaultLibPathFile.getAbsolutePath());
            }
            // match jar files with pattern
            List<String> patternJars = ZipFileUtils.unzipFilesFromArchiva(fatJarFile, "jar", pattern, defaultLibPathFile);
            for (String jar : patternJars) {
                // indexing all .class files
                var jarIndexes = IndexingUtils.indexingJarFile(new File(jar), "class", "", targetIndexesPath, false);
                allIndexes.addAll(jarIndexes);
            }
        }

        return SuccessTip.create(allIndexes);
    }


    @PostMapping("/deploy")
    @ApiOperation(value = "直接部署.class/.jar文件")
    public Tip uploadJarFile(@RequestPart("file") MultipartFile jarOrClass) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        File rootPathFile = new File(rootPath);
        Assert.isTrue(rootPathFile.exists(), "jar-dependency:root-path: 配置项不存在！");
        Assert.isTrue(jarOrClass!=null && !jarOrClass.isEmpty(), "部署文件不能为空");
        String fileType = FileUtils.extension(jarOrClass.getOriginalFilename());
        Assert.isTrue(fileType.equals("class") || fileType.equals("jar"), "only support .class or .jar !");

        // TODO,
        // 通过docker api endpoint直接部署到指定容器内部
        String container = jarDeployProperties.getContainer();
        String endpoint = jarDeployProperties.getDockerApiEndpoint();

        String defaultClassesPath = "classes";
        String defaultLibPath  ="lib";

        File defaultLibFilePath = new File(String.join(File.separator, rootPath, defaultLibPath));
        String deployToDir = "class".equals(fileType)?
                String.join(File.separator, rootPath, defaultClassesPath) :
                String.join(File.separator, rootPath, defaultLibPath);
        File uploadedFile = UploadUtils.doMultipartFile(jarOrClass, deployToDir);

        // deploy result
        String deployedPath = uploadedFile.getAbsolutePath().substring(rootPathFile.getAbsolutePath().length());


        // TODO, restart container via RestTemplate


        return SuccessTip.create(deployedPath);
    }

    @GetMapping("/decompile")
    @ApiOperation(value = "反编译指定的文件(pattern空,即显示jar所有文件")
    public Tip decompileJarFile(@RequestParam(value = "jar") String jar,
                                @RequestParam(value = "pattern", required = false) String pattern,
                                HttpServletResponse response
    ) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-dependency:root-path: 没有配置！");
        String classesPath = "classes";

        List<String> files = null;
        File jarFile = new File(String.join(File.separator, rootPath, jar));
        Assert.isTrue(jarFile.exists(), jar + " not exists!");

        //TODO, decompile .class file in .jar within fat jar.


        // show all files is pattern is empty
        if (org.apache.commons.lang3.StringUtils.isBlank(pattern)) {
            return SuccessTip.create(ZipFileUtils.listEntriesFromArchive(jarFile, "", pattern));
        }

        File classesPathFile = new File(String.join(File.separator, rootPath, classesPath));
        var unzipFiles = ZipFileUtils.unzipFilesFromArchiva(jarFile, "", pattern, classesPathFile);
        files = unzipFiles.stream()
                .filter(f -> FilenameUtils.getExtension(f).equals("class"))
                .map(
                        f -> String.join(File.separator, rootPath, f)
                )
                .collect(Collectors.toList());
        if(files==null){
            files = new ArrayList<>();
        }

        // start to decompile
        List<String> decompiles = DecompileUtils.decompileFiles(files, false);

        // output to browser
        ServletOutputStream out = response.getOutputStream();
        for(String line : decompiles){
            out.println(line);
        }
        out.close();

        return SuccessTip.create(files.size());
    }
}
