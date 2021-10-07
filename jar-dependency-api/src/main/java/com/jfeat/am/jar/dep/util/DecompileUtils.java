package com.jfeat.am.jar.dep.util;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DecompileUtils {
    public static String decompileFiles(List<String> files, Boolean forceClean){
        if(forceClean==null) forceClean = false;

        // decompile
        final StringBuilder lines = new StringBuilder();
        OutputSinkFactory.Sink println = line -> {
            lines.append(line);
            System.out.println(line);
        };

        OutputSinkFactory mySink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                // I only understand how to sink strings, regardless of what you have to give me.
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                return sinkType == SinkType.JAVA ? println : ignore -> {
                };
            }
        };

        CfrDriver cfrDriver = new CfrDriver.Builder().withOutputSink(mySink).build();
        //CfrDriver cfrDriver = (new CfrDriver.Builder()).build();
        cfrDriver.analyse(files);

        if (forceClean) {
            files.stream().forEach(
                    filePath -> {
                        org.codehaus.plexus.util.FileUtils.fileDelete(filePath);
                        try {
                            String dirname = org.codehaus.plexus.util.FileUtils.dirname(filePath);
                            File dirFile = new File(dirname);
                            if (dirFile.listFiles().length == 0) {
                                org.codehaus.plexus.util.FileUtils.forceDelete(dirFile);
                            }
                        }catch (IOException e){
                        }
                    }
            );
        }

        return lines.toString();
    }
}
