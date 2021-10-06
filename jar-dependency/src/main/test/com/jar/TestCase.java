package com.jar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.jfeat.jar.dependency.FileUtils.getRelativePath;
import static java.util.function.Predicate.not;

public class TestCase {
    Logger logger = Logger.getLogger(TestCase.class.getSimpleName());

    List<String> origin = null;
    List<String> target = null;

    @Before
    public void before(){
        origin = Arrays.asList(new String[]{"spring-test-1.5.6-RELEASE.jar", "crud-core-0.0.1.jar", "junit-4.12.jar"});
        target = Arrays.asList(new String[]{"junit-${junit.version}.jar", "crud-core-0.0.1.jar"});
    }

    @Test
    public void TestMismatch(){
        var query = target.stream().filter(not(origin::contains))
                .collect(Collectors.toList());
        Assert.assertTrue(query.size()==1);
        Assert.assertTrue(query.get(0).equals("junit-${junit.version}.jar"));
        logger.info("mismatches="+ query.get(0).toString());
    }

    @Test
    public void TestMatches(){
        var query = target.stream().filter(origin::contains)
                .collect(Collectors.toList());
        Assert.assertTrue(query.size()==1);
        Assert.assertTrue(query.get(0).equals("crud-core-0.0.1.jar"));
        logger.info("matches="+ query.get(0).toString());
    }

    @Test
    public void TestSkipVersion(){
        final String regrex = "(-[0-9\\.]+)([\\-\\.\\w]*).jar";  //replace -1.0.0
        final String regrex$ = "(-\\$\\{[\\w\\.\\-]+\\}).jar";   //replace -${junit.version}
        var query = target.stream()
                .map(u->u.replaceFirst(regrex, ".jar").replaceFirst(regrex$,".jar"))
                .collect(Collectors.toList());

        Assert.assertTrue(query.get(0).equals("junit.jar"));
        Assert.assertTrue(query.get(1).equals("crud-core.jar"));
        query.forEach(u->{System.out.println(u.toString());});
    }

    @Test
    public void TestMismatchSkipVersion(){
        final String regrex = "(-[0-9\\.]+)([\\-\\.\\w]*).jar";
        final String regrex$ = "(-\\$\\{[\\w\\.\\-]+\\}).jar";
        var queryOrigin = origin.stream()
                .map(u->u.replaceFirst(regrex, ".jar").replaceFirst(regrex$,".jar"))
                .collect(Collectors.toList());
        //queryOrigin.forEach(u->{System.out.println(u.toString());});

        var queryTargetStream = target.stream()
                .map(u->u.replaceFirst(regrex, ".jar").replaceFirst(regrex$,".jar"));
                //.collect(Collectors.toList());

        var query = queryTargetStream.filter(not(queryOrigin::contains))
                .collect(Collectors.toList());
        query.forEach(u->{System.out.println(u.toString());});
    }

    @Test
    public void TestRelativeFilePath(){
        String result = getRelativePath("/path/to/one/filename.jar", "/path/to/two/filename.class");
        System.out.println(result);
        Assert.assertTrue(result.equals("two/filename.class"));
    }

}
