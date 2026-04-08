package com.tzy.sky.controller;

import com.tzy.sky.dto.base.Result;
import com.tzy.sky.service.PhysicsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ue")
@CrossOrigin
@Tag(name = "UE引擎控制", description = "UE5仿真引擎启动停止控制")
public class UeController {

    private final PhysicsService physicsService;

    public UeController(PhysicsService physicsService) {
        this.physicsService = physicsService;
    }

    @Operation(summary = "启动UE5仿真引擎", description = "启动UE5像素流仿真引擎，需要配置正确的路径")
    @GetMapping("/start")
    public ResponseEntity<Result<String>> startUe() {
        try {
            Result<String> result = physicsService.startUeSystem();
            if (result.getCode() != 200) {
                return ResponseEntity.badRequest().body(result);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Result.error(500, "UE引擎启动失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "停止UE5仿真引擎", description = "停止UE5像素流仿真引擎")
    @GetMapping("/stop")
    public ResponseEntity<Result<String>> stopUe() {
        try {
            Result<String> result = physicsService.stopUeSystem();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Result.error(500, "UE引擎停止失败: " + e.getMessage()));
        }
    }
}