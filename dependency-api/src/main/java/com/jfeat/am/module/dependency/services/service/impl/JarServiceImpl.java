package com.jfeat.am.module.dependency.services.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jfeat.am.module.dependency.services.persistence.dao.JarMapper;
import com.jfeat.am.module.dependency.services.persistence.model.Jar;
import com.jfeat.am.module.dependency.services.service.JarService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zxchengb
 * @since 2020-08-05
 */
@Service
public class JarServiceImpl extends ServiceImpl<JarMapper, Jar> implements JarService {
    /**
     * JAR实体映射对象
     */
    private JarMapper jarMapper;

    public JarServiceImpl(JarMapper jarMapper) {
        this.jarMapper = jarMapper;
    }

    /**
     * 根据应用标识ID查询JAR包详细记录
     *
     * @param appId 目标应用标识ID
     * @return com.jfeat.am.module.dependency.services.persistence.model.Jar
     */
    @Override
    public Jar selectByAppId(String appId) {
        Jar jarCondition = new Jar();
        jarCondition.setAppId(appId);
        return jarMapper.selectList(new LambdaQueryWrapper<>(jarCondition)).stream().findFirst().orElse(null);
    }

    @Override
    public List<Jar> selectAll() {
        return jarMapper.selectList(new QueryWrapper<>());
    }
}
