package com.tzy.sky.service;

import com.tzy.sky.config.properties.SatelliteSimulationProperties;
import com.tzy.sky.dto.base.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UeProcessService {

    private static final String UE_URL = "http://127.0.0.1:80/minimal.html";

    private final SatelliteSimulationProperties config;
    private final ExecutorService executorService;
    private Process signallingProcess = null;
    private Process ueProcess = null;
    private String signallingPath;
    private String ueExePath;

    public UeProcessService(SatelliteSimulationProperties config) {
        this.config = config;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void initialize(String signallingPath, String ueExePath) {
        this.signallingPath = signallingPath;
        this.ueExePath = ueExePath;
    }

    public Result<String> startUeSystem() {
        log.info("=== UE系统启动请求 ===");
        log.info("signallingPath: {}", signallingPath);
        log.info("ueExePath: {}", ueExePath);

        if (ueProcess != null && ueProcess.isAlive()) {
            return Result.success("UE系统已在运行", UE_URL);
        }

        if (signallingPath == null || signallingPath.trim().isEmpty()) {
            log.error("信令服务器脚本路径未配置或为null");
            return Result.error(400, "信令服务器脚本路径未配置");
        }

        if (ueExePath == null || ueExePath.trim().isEmpty()) {
            log.error("UE5可执行文件路径未配置或为null");
            return Result.error(400, "UE5可执行文件路径未配置");
        }

        executorService.submit(() -> {
            try {
                startSignallingServer();
                startUeInstance();
            } catch (Exception e) {
                log.error("UE 系统启动异常", e);
                cleanupProcesses();
            }
        });

        return Result.success("UE系统启动中", UE_URL);
    }

    private void startSignallingServer() throws Exception {
        log.info("启动信令服务器...");
        log.info("脚本路径: {}", signallingPath);

        File scriptFile = new File(signallingPath);
        if (!scriptFile.exists()) {
            log.error("信令服务器脚本文件不存在: {}", signallingPath);
            return;
        }

        ProcessBuilder pbSig = new ProcessBuilder("cmd.exe", "/c", signallingPath);
        pbSig.inheritIO();
        this.signallingProcess = pbSig.start();

        log.info("等待信令服务器启动...");
        TimeUnit.SECONDS.sleep(config.getUe5().getSignalling().getStartupDelay());
    }

    private void startUeInstance() throws IOException {
        log.info("启动 UE 实例...");
        log.info("UE5路径: {}", ueExePath);

        File ueFile = new File(ueExePath);
        if (!ueFile.exists()) {
            log.error("UE5可执行文件不存在: {}", ueExePath);
            return;
        }

        List<String> args = new ArrayList<>(Arrays.asList(
            ueExePath,
            "-PixelStreamingURL=" + config.getUe5().getExecutable().getPixelStreamingUrl(),
            "-ForceRes",
            "-ResX=" + config.getUe5().getExecutable().getResolution().getWidth(),
            "-ResY=" + config.getUe5().getExecutable().getResolution().getHeight(),
            config.getUe5().getExecutable().getWindow().getMode(),
            config.getUe5().getExecutable().getWindow().getRenderOffScreen(),
            "-ccmd=\"r.ScreenPercentage " + config.getUe5().getExecutable().getPerformance().getScreenPercentage() + "\"",
            "-PixelStreamingEncoderRateControl=" + config.getUe5().getExecutable().getPerformance().getEncoderRateControl(),
            "-PixelStreamingEncoderBitrate=" + config.getUe5().getExecutable().getPerformance().getEncoderBitrate(),
            "-PixelStreamingEncoderMaxQP=" + config.getUe5().getExecutable().getPerformance().getEncoderMaxQp(),
            "-WinY=" + config.getUe5().getExecutable().getWindow().getWinY(),
            config.getUe5().getExecutable().getWindow().getAllowSecondaryDisplays(),
            config.getUe5().getExecutable().getAudio()
        ));

        log.info("UE5启动参数: {}", String.join(" ", args));

        ProcessBuilder pbUE = new ProcessBuilder(args);
        pbUE.inheritIO();
        this.ueProcess = pbUE.start();

        log.info("UE 实例启动成功");
    }

    public synchronized Result<String> stopUeSystem() {
        boolean killed = cleanupProcesses();
        log.info("UE 外部进程清理完成");
        return killed ? Result.success("UE系统已停止", "进程已清理") : Result.success("无需清理", "没有运行中的进程");
    }

    private boolean cleanupProcesses() {
        boolean killed = false;
        if (ueProcess != null) {
            ueProcess.destroyForcibly();
            ueProcess = null;
            killed = true;
        }
        if (signallingProcess != null) {
            signallingProcess.destroyForcibly();
            signallingProcess = null;
            killed = true;
        }
        try {
            Runtime.getRuntime().exec("taskkill /F /IM node.exe");
        } catch (IOException ignored) {
        }
        return killed;
    }

    public boolean isUeProcessRunning() {
        return ueProcess != null && ueProcess.isAlive();
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}