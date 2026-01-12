package com.github.shy526.vo;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PageModel {
    private String NextKey;
    private Boolean hasMore=false;
    private List<JSONObject> Data=new ArrayList<>();
}
