package com.tzy.sky.dto.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO implements Serializable {
    private double x;
    private double y;
    private double z;
}