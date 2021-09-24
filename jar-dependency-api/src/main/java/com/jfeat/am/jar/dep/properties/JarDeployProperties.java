package com.jfeat.am.jar.dep.properties;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 存储桶关键属性配置
 *
 * @author vincent
 * @date 2021-08-20
 */
@Data
@Component
@Accessors(chain = true)
@ConfigurationProperties(prefix = "jar-dependency")
public class JarDeployProperties {
    /**
     * 应用路径
     */
    String rootPath;

    /**
     *  用于初始化数据库的路径
     */
    String flywayPath;
}