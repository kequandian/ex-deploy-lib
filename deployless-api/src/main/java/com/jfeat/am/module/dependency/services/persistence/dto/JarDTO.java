package com.jfeat.am.module.dependency.services.persistence.dto;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Jar实体信息传输类
 * @author zxchengb
 * @date 2020-08-05
 */
@Data
@Accessors(chain = true)
public class JarDTO {
    /**
     * 应用标识ID
     */
    private String appId;
    /**
     * JAR包依赖
     */
    private List<String> dependencies;

    /**
     * 匹配方式：true -> 匹配不同（默认） , false -> 匹配相同
     */
    //private Boolean verbose = true;
}