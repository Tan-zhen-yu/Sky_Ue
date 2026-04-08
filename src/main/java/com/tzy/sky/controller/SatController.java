package com.tzy.sky.controller;

import com.tzy.sky.dto.base.Result;
import com.tzy.sky.exception.BusinessException;
import com.tzy.sky.service.PhysicsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sat")
@CrossOrigin
@Tag(name = "卫星控制", description = "卫星轨道、姿态、发动机等控制接口")
public class SatController {

    @Autowired
    private PhysicsService physicsService;

    @Operation(summary = "设置卫星轨道半径", description = "修改指定卫星的轨道高度")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "轨道设置成功",
                    content = @Content(schema = @Schema(example = "{\"code\":200,\"message\":\"操作成功\",\"data\":\"SAT-001 Orbit Radius Set to: 8000.0\",\"timestamp\":\"2026-04-08T13:30:00\"}"))),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/orbit")
    public Result<String> setOrbit(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @RequestParam String id,
            @Parameter(description = "轨道高度(km)", required = true, example = "8000")
            @RequestParam double alt) {
        if (id == null || id.trim().isEmpty()) {
            throw BusinessException.invalidParameter("卫星ID不能为空");
        }
        if (alt < 0) {
            throw BusinessException.invalidParameter("轨道高度不能为负数");
        }
        physicsService.setOrbit(id, alt);
        return Result.success("轨道已更新", id + " Orbit Radius Set to: " + alt);
    }

    @Operation(summary = "设置卫星姿态", description = "控制卫星的俯仰、偏航、翻滚角")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "姿态设置成功"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/attitude")
    public Result<String> setAttitude(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @RequestParam String id,
            @Parameter(description = "旋转轴(pitch/yaw/roll)", required = true, example = "pitch")
            @RequestParam String axis,
            @Parameter(description = "角度值", required = true, example = "45.0")
            @RequestParam double val) {
        if (id == null || id.trim().isEmpty()) {
            throw BusinessException.invalidParameter("卫星ID不能为空");
        }
        if (axis == null || axis.trim().isEmpty()) {
            throw BusinessException.invalidParameter("旋转轴不能为空");
        }
        physicsService.setAttitude(id, axis, val);
        return Result.success("姿态已更新", id + " " + axis + " Set to: " + val);
    }

    @Operation(summary = "更新传感器状态", description = "报告卫星传感器的遮挡状态")
    @PostMapping("/sensor")
    public Result<String> reportSensor(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @RequestParam String id,
            @Parameter(description = "是否被遮挡", required = true)
            @RequestParam boolean occluded) {
        if (id == null || id.trim().isEmpty()) {
            throw BusinessException.invalidParameter("卫星ID不能为空");
        }
        physicsService.updateSensorStatus(id, occluded);
        return Result.success("传感器状态已更新", "Sensor Status Updated: " + (occluded ? "Occluded" : "Clear"));
    }

    @Operation(summary = "设置卫星速度", description = "调整卫星的运行速度倍率")
    @PostMapping("/speed")
    public Result<String> setSpeed(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @RequestParam String id,
            @Parameter(description = "速度值", required = true, example = "7.5")
            @RequestParam double speed) {
        if (id == null || id.trim().isEmpty()) {
            throw BusinessException.invalidParameter("卫星ID不能为空");
        }
        if (speed < 0) {
            throw BusinessException.invalidParameter("速度不能为负数");
        }
        physicsService.setSpeed(id, speed);
        return Result.success("速度已更新", id + " Speed Scale Set to: " + speed);
    }

    @Operation(summary = "控制发动机", description = "启动或关闭卫星发动机")
    @PostMapping("/engine")
    public Result<String> setEngine(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @RequestParam String id,
            @Parameter(description = "是否启动", required = true)
            @RequestParam boolean active) {
        if (id == null || id.trim().isEmpty()) {
            throw BusinessException.invalidParameter("卫星ID不能为空");
        }
        physicsService.setEngineState(id, active);
        return Result.success("发动机状态已更新", "Engine Set: " + active);
    }

    @Operation(summary = "拍照", description = "触发卫星相机拍照，电量不足时无法执行")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "拍照成功"),
            @ApiResponse(responseCode = "400", description = "电量不足"),
            @ApiResponse(responseCode = "404", description = "卫星不存在")
    })
    @PostMapping("/photo")
    public Result<String> takePhoto(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @RequestParam String id) {
        if (id == null || id.trim().isEmpty()) {
            throw BusinessException.invalidParameter("卫星ID不能为空");
        }
        String result = physicsService.triggerCamera(id);
        if (result.contains("不存在")) {
            throw BusinessException.satelliteNotFound(id);
        }
        if (result.contains("电量不足")) {
            throw BusinessException.lowBattery();
        }
        return Result.success("拍照成功", result);
    }

    @Operation(summary = "设置聚焦目标", description = "将相机聚焦到指定卫星")
    @PostMapping("/focus")
    public Result<String> setFocus(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @RequestParam String id) {
        if (id == null || id.trim().isEmpty()) {
            throw BusinessException.invalidParameter("卫星ID不能为空");
        }
        String result = physicsService.setFocusTarget(id);
        if (result.contains("不存在")) {
            throw BusinessException.satelliteNotFound(id);
        }
        return Result.success("聚焦目标已设置", result);
    }

    @Operation(summary = "设置指向模式", description = "控制卫星的指向模式(对日/对地/自由)")
    @PostMapping("/pointing")
    public Result<String> setPointing(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @RequestParam String id,
            @Parameter(description = "指向模式(sun/earth/free)", required = true, example = "earth")
            @RequestParam String target) {
        if (id == null || id.trim().isEmpty()) {
            throw BusinessException.invalidParameter("卫星ID不能为空");
        }
        if (target == null || target.trim().isEmpty()) {
            throw BusinessException.invalidParameter("指向模式不能为空");
        }
        physicsService.setPointingMode(id, target);
        return Result.success("指向模式已更新", id + " Pointing Mode Set to: " + target);
    }

    @Operation(summary = "设置热控模式", description = "控制卫星的热控系统模式")
    @PostMapping("/thermal")
    public Result<String> setThermal(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @RequestParam String id,
            @Parameter(description = "热控模式(auto/heater/cooler)", required = true, example = "auto")
            @RequestParam String mode) {
        if (id == null || id.trim().isEmpty()) {
            throw BusinessException.invalidParameter("卫星ID不能为空");
        }
        if (mode == null || mode.trim().isEmpty()) {
            throw BusinessException.invalidParameter("热控模式不能为空");
        }
        physicsService.setThermalMode(id, mode);
        return Result.success("热控模式已更新", id + " Thermal Mode Set to: " + mode);
    }
}