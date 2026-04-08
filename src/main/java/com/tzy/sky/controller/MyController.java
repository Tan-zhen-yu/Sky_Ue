package com.tzy.sky.controller;


import com.tzy.sky.service.PhysicsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MyController {


    @Autowired
    PhysicsService physicsService;


    @GetMapping("/play")
    public void play() {
        physicsService.tick();
    }
}