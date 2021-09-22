package com.jfeat.am.module.dependency.services.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jfeat.am.module.dependency.services.persistence.model.Jar;

import java.util.List;

/**
 *  JAR包服务类
 *
 * @author zxchengb
 * @since 2020-08-05
 */
public interface JarService extends IService<Jar> {
    /**
     * 根据应用标识ID查询JAR包详细记录
     * @param appId 目标应用标识ID
     * @return com.jfeat.am.module.dependency.services.persistence.model.Jar
     */
    Jar selectByAppId(String appId);

    List<Jar> selectAll();
}
