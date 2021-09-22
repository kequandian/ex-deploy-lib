package com.jfeat.am.module.dependency.services.persistence.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jfeat.am.module.dependency.services.persistence.model.Jar;

import org.springframework.stereotype.Component;

/**
 * Jar包依赖Mapper 接口
 *
 * @author zxchengb
 * @since 2020-08-05
 */
@Component
public interface JarMapper extends BaseMapper<Jar> {

}
