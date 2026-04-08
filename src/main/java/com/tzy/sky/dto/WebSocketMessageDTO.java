package com.tzy.sky.dto;

import lombok.Data;

@Data
public class WebSocketMessageDTO {
    public String target;
    public String action;
    public Object payload; // 这里用 Object 是为了通用，也可以用泛型
}
