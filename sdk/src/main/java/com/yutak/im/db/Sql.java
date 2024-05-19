package com.yutak.im.db;

import java.util.List;

public class Sql {
    // sql list
    public List<String> s;
    // index
    public long i;

    public Sql(long i, List<String> s){
        this.i = i;
        this.s = s;
    }
}
