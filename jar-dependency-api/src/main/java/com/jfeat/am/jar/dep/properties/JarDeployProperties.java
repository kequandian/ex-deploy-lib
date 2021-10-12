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
@ConfigurationProperties(prefix = "jarDependency")
public class JarDeployProperties {
    /**
     * 容器内应用路径
     */
    String rootPath;

    /**
     * 容器名称/ID
     */
    String container;

    /**
     * Docker API endpoint
     */
    String dockerApiEndpoint;

    /**
     *  用于初始化数据库的路径
     */
    String flywayPath;

    public String getRootPath(){
        return this.rootPath;
    }
    public void setRootPath(String rootPath){
        this.rootPath = rootPath;
    }
    public String getDockerApiEndpoint(){
        return this.dockerApiEndpoint;
    }
    public void setDockerApiEndpoint(String endpoint){
        this.dockerApiEndpoint = endpoint;
    }
    public String getContainer(){
        return this.container;
    }
    public void setContainer(String container){
        this.container = container;
    }
    public String getFlywayPath(){
        return this.flywayPath;
    }
    public void setFlywayPath(String flywayPath){
        this.flywayPath = flywayPath;
    }
}