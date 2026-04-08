package com.tzy.sky.controller;

import com.tzy.sky.service.PhysicsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimulationController {

    private final PhysicsService physicsService;

    public SimulationController(PhysicsService physicsService) {
        this.physicsService = physicsService;
    }

    @GetMapping("/simulation/play")
    public void play() {
        physicsService.tick();
    }

    @GetMapping("/simulation/pause")
    public void pause() {
        physicsService.setTimeScale(0.0);
    }
}