package com.example.epark.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by 小言 on 2017/9/13.
 */

public class Now {
    @SerializedName("tmp")
    public String temperature;

    @SerializedName("cond")
    public More more;

    public class More {
        @SerializedName("txt")
        public String info;
    }
}
