package com.jfeat.am.module.dependency.properties;

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
@ConfigurationProperties(prefix = "deployless")
public class DeploylessProperties {
    /**
     * 应用路径
     */
    String appsPath;

    public File getStandaloneFile(){
        File file = new File(appsPath);
        File[] listFiles = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                try {
                    String path = file.getCanonicalPath();
                    return FilenameUtils.getExtension(path).equals("jar") &&
                            (FilenameUtils.getBaseName(path).equals("app") || FilenameUtils.getBaseName(path).endsWith("standalone"));
                }catch (IOException e){
                    return false;
                }
            }
        });
        if (listFiles!=null && listFiles.length>1)
            return listFiles[0];

        throw new BusinessException(BusinessCode.FileReadingError);
    }

    /**
     * 库目录
     */
    String libPath;

    /**
     * 代码路径
     */
    String codePath;


    /**
     *  用于初始化数据库的路径
     */
    String flywayPath;
}