package com.tzy.sky.dto.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotationDTO implements Serializable {
    private double p; // Pitch
    private double r; // Roll
    private double y; // Yaw
}