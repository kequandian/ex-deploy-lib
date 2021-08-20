import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfeat.AmApplication;
import com.jfeat.am.module.dependency.services.persistence.model.Jar;
import com.jfeat.am.module.dependency.services.service.impl.JarServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;

/**
 * @author zxchengb
 * @date 2020-08-05
 */
@SpringBootTest(classes = AmApplication.class)
@RunWith(SpringRunner.class)
public class DependenciesTest {
    @Autowired
    private JarServiceImpl jarService;

    //@Test
    public void test(){
        Jar jar = jarService.selectByAppId("UjQhM2mOOR7I");
        JSONObject originDependencies = JSONObject.parseObject(jar.getDependencies());
        System.out.println(JSONArray.parseArray(originDependencies.getString("dependencies")));
    }
}