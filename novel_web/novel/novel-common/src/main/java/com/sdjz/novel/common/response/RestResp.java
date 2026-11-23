package com.sdjz.novel.common.response;

import lombok.Data;

@Data
public class RestResp<T> {
    private String code;
    private T data;
    private String message;

    public static <T> RestResp<T> ok(T data){
        RestResp<T> resp = new RestResp<>();
        resp.setCode("00000");
        resp.setData(data);
        resp.setMessage("成功");
        return resp;
    }
    public static <T> RestResp<T> ok(){
        return ok(null);
    }

    public static <T> RestResp<T> fail(String code,String message){
        RestResp<T> resp = new RestResp<>();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }
}

