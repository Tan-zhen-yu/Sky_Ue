package com.tzy.sky.controller;

import com.tzy.sky.service.PhysicsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ue") // 新的路径
public class UeController {

    @Autowired
    private PhysicsService physicsService;

    @GetMapping("/start")
    public String startUe() {
        return physicsService.startUeSystem();
    }

    @GetMapping("/stop")
    public String stopUe() {
        return physicsService.stopUeSystem();
    }
}