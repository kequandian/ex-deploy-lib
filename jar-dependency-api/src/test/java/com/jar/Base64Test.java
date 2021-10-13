package com.jar;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Base64;

public class Base64Test{
    Logger logger = LoggerFactory.getLogger(Base64Test.class.getSimpleName());

    @Test
    public void TestDecode(){
        String str=  "\\myString.java";
        String res = StringUtils.stripStart(str, File.separator);

        String baseJar64 = "amFyLWRlcGVuZGVuY3ktYXBpLTEuMC4wLXN0YW5kYWxvbmUuamFy";
        String baseJar = new String(Base64.getDecoder().decode(baseJar64));
        logger.info(baseJar);

        Assert.isTrue("jar-dependency-api-1.0.0-standalone.jar".equals(baseJar));
    }
}