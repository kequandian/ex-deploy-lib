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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/jar/dep/sugar")
public class IndexingJarDeployEndpoint {
    protected final static Logger logger = LoggerFactory.getLogger(IndexingJarDeployEndpoint.class);

    @Autowired
    private JarDeployProperties jarDeployProperties;

    @PostMapping("/deploy")
    @ApiOperation(value = "直接部署.class/.jar文件")
    public Tip uploadJarFile(@RequestPart("file") MultipartFile jarOrClass) throws IOException {
        String rootPath = jarDeployProperties.getRootPath();
        File rootPathFile = new File(rootPath);
        Assert.isTrue(rootPathFile.exists(), "jar-deploy:root-path: 配置项不存在！");
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
        Assert.isTrue(StringUtils.isNotBlank(rootPath), "jar-deploy:root-path: 没有配置！");
        String classesPath = "classes";

        List<String> files = null;
        File jarFile = new File(String.join(File.separator, rootPath, jar));
        Assert.isTrue(jarFile.exists(), jar + " not exists!");

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
