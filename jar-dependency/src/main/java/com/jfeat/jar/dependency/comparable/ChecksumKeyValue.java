package com.jfeat.jar.dependency.comparable;

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Map;

public class ChecksumKeyValue<K,V> implements Comparable<K>, Map.Entry<K, V> {

    private Map.Entry<K,V> holder;

    public ChecksumKeyValue(K k, V v){
        holder = Map.entry(k,v);
    }

    @Override
    public int compareTo(K k) {
        return FileUtils.filename(holder.getKey().toString().replace("/", File.separator))
                .compareTo(FileUtils.filename(k.toString().replace("/", File.separator)));
    }

    @Override
    public K getKey() {
        return holder.getKey();
    }

    @Override
    public V getValue() {
        return holder.getValue();
    }

    @Override
    public V setValue(V value) {
        return holder.setValue(value);
    }

    @Override
    public boolean equals(Object o) {
        return holder.equals(o);
    }

    @Override
    public int hashCode() {
        return holder.hashCode();
    }
}
