package com.tzy.sky.controller;


import com.tzy.sky.service.PhysicsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sim")
@CrossOrigin // 允许跨域，方便本地测试
public class SimulationController {

    @Autowired
    private PhysicsService physicsService;

    // 设置全局时间流速 (例如 0.0 暂停, 1.0 正常, 10.0 十倍速)
    @PostMapping("/timeScale")
    public String setTimeScale(@RequestParam double scale) {
        physicsService.setTimeScale(scale);
        return "Global TimeScale Set to: " + scale;
    }

    // 手动触发一次 Tick (调试用)
    @GetMapping("/tick")
    public String manualTick() {
        physicsService.tick();
        return "Manual tick executed";
    }

    @PostMapping("/viewEarth")
    public String viewEarth() {
        physicsService.viewEarth();
        return "全局观察";
    }
}