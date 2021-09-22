package com.jfeat.am.module.dependency.api;

import com.jfeat.am.module.dependency.services.persistence.dto.JarDTO;
import com.jfeat.am.module.dependency.services.persistence.model.Jar;
import com.jfeat.am.module.dependency.services.service.impl.JarServiceImpl;
import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.tips.ErrorTip;
import com.jfeat.crud.base.tips.SuccessTip;
import com.jfeat.crud.base.tips.Tip;

import com.jfeat.jar.dependency.DependencyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 依赖处理接口
 *
 * @author zxchengb
 * @date 2020-08-05
 */
@RestController
@Api("/api/jar/dependencies")
@RequestMapping("/api/jar/dependencies")
public class DependencyEndpoint {
    /**
     * JAR包服务类对象
     */
    private JarServiceImpl jarService;

    public DependencyEndpoint(JarServiceImpl jarService) {
        this.jarService = jarService;
    }

    @GetMapping
    @ApiOperation(value = "获取所有应用依赖列表", response = Jar.class)
    public Tip getDependency() {
        return SuccessTip.create(jarService.selectAll());
    }

    @GetMapping("/{appId}")
    @ApiOperation(value = "根据应用标识ID获取依赖（Dependency）详情（JSON）", response = Jar.class)
    @ApiParam(value = "appId", name = "待查询的应用标识ID")
    public Tip getDependency(@PathVariable String appId) {
        return SuccessTip.create(jarService.selectByAppId(appId));
    }


    @PostMapping("/match")
    @ApiOperation(value = "上传的JAR包依赖与应用ID源JAR包的匹配项", response = HashMap.class)
    @ApiParam(value = "jarDTO", name = "Jar传输对象")
    public Tip match(@RequestBody JarDTO jarDTO) {
        try {
            return SuccessTip.create()
                    .setData(DependencyUtils.getSameDependencies(jarService.selectByAppId(jarDTO.getAppId()).getListDependencies(),jarDTO.getDependencies()));
        } catch (Exception e) {
            return ErrorTip.create(BusinessCode.SyntaxError.getCode(), e.getMessage());
        }
    }

    @PostMapping("/mismatch")
    @ApiOperation(value = "上传的JAR包依赖中与应用ID源JAR包的不匹配的依赖", response = HashMap.class)
    @ApiParam(value = "jarDTO", name = "Jar传输对象")
    public Tip mismatch(@RequestBody JarDTO jarDTO) {
        try {
            return SuccessTip.create()
                    .setData(DependencyUtils.getDifferentDependencies(jarService.selectByAppId(jarDTO.getAppId()).getListDependencies(),jarDTO.getDependencies()));
        } catch (Exception e) {
            return ErrorTip.create(BusinessCode.SyntaxError.getCode(), e.getMessage());
        }
    }

    @PostMapping("/inject")
    @ApiOperation(value = "上传待注入的依赖(Dependency) JSON，由本接口进行分析、判断及注入", response = HashMap.class)
    @ApiParam(value = "jarDTO", name = "Jar传输对象")
    public Tip inject(@RequestBody JarDTO jarDTO) {
        Tip tip;
        Jar jar = jarService.selectByAppId(jarDTO.getAppId());
        List<String> originDependencies = jar.getListDependencies();
        List<String> targetDependencies = jarDTO.getDependencies();
        List<String> errorParams = DependencyUtils.getErrorDependencies(targetDependencies);
        if (!errorParams.isEmpty()) {
            return ErrorTip.create(BusinessCode.SyntaxError).add("errorParams", errorParams.toString());
        } else {
            originDependencies.addAll(DependencyUtils.getDifferentDependencies(originDependencies,targetDependencies));
            tip = jarService.updateById(jar.setJsonDependencies(originDependencies)) ? SuccessTip.create() : ErrorTip.create(BusinessCode.CRUD_UPDATE_FAILURE);
        }
        return tip;
    }
}