package com.jar;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Base64;

public class Base64Test{
    Logger logger = LoggerFactory.getLogger(Base64Test.class.getSimpleName());

    @Test
    public void TestDecode(){
        String baseJar64 = "amFyLWRlcGxveS1hcGktMS4wLjAtc3RhbmRhbG9uZS5qYXI=";
        String baseJar = new String(Base64.getDecoder().decode(baseJar64));
        logger.info(baseJar);

        Assert.isTrue("jar-deploy-api-1.0.0-standalone.jar".equals(baseJar));
    }
}