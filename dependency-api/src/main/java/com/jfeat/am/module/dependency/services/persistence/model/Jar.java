package com.jfeat.am.module.dependency.services.persistence.model;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * JAR实体对象
 *
 * @author zxchengb
 * @since 2020-08-05
 */
@Data
@Accessors(chain = true)
@TableName("t_jar")
@EqualsAndHashCode(callSuper = true)
public class Jar extends Model<Jar> {
    private static final long serialVersionUID = 1L;
    /**
     * 记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 应用标识ID
     */
    private String appId;
    /**
     * JAR包记录名称
     */
    private String name;
    /**
     * 当前记录状态
     */
    private String status;
    /**
     * JAR包依赖
     */
    @JsonIgnore
    private String dependencies;

    /**
     * 返回List形式的依赖集合
     *
     * @return java.util.List<java.lang.String>
     */
    public List<String> getListDependencies() {
        JSONObject originDependenciesJson = JSONObject.parseObject(getDependencies());
        return JSONArray.parseArray(originDependenciesJson.getString("dependencies"), String.class);
    }

    /**
     * 将集合依赖转换为JSON并设置进该实体类中
     * @param dependencies 待转换的依赖
     * @return com.jfeat.am.module.dependency.services.persistence.model.Jar
     */
    public Jar setJsonDependencies(List<String> dependencies) {
        JSONObject jsonObject = new JSONObject(1);
        jsonObject.put("dependencies", dependencies);
        return setDependencies(JSON.toJSONString(jsonObject, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat));
    }

    @Override
    protected Serializable pkVal() {
        return this.id;
    }
}


