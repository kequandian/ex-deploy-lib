package com.jfeat.am.jar.deploy.properties;

import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.io.FilenameUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * 存储桶关键属性配置
 *
 * @author vincent
 * @date 2021-08-20
 */
@Data
@Component
@Accessors(chain = true)
@ConfigurationProperties(prefix = "jar-deploy")
public class JarDeployProperties {
    /**
     * 应用路径
     */
    String rootPath;

    // public File getStandaloneFile(){
    //     File file = new File(appsPath);
    //     File[] listFiles = file.listFiles(new FileFilter() {
    //         @Override
    //         public boolean accept(File file) {
    //             String fileName = file.getName();
    //             return fileName.endsWith("app.jar") ||
    //                    fileName.endsWith ("-standalone.jar");
    //         }
    //     });
    //     if (listFiles!=null && listFiles.length>0)
    //         return listFiles[0];

    //     return null;
    // }


    /**
     *  用于初始化数据库的路径
     */
    String flywayPath;
}