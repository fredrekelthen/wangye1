package com.edu.backend.controller;

import com.edu.backend.service.PythonEmulationService;
import com.edu.backend.utils.BaseResponse;
import com.edu.backend.utils.ResultUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 电磁仿真接口（转发至 qigger 独立后端）。
 *
 * <p>仿真算法已由 MATLAB 迁移为 Python 独立服务（qigger/python-emulation）。
 * 本控制器对外提供统一的仿真入口，由 javahouduan 后端通过
 * {@link PythonEmulationService} 调用该服务的 /student/* 接口完成计算。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/emulation")
public class EmulationController {

    @Resource
    private PythonEmulationService pythonEmulationService;

    /**
     * 双极子天线方向图仿真。
     *
     * @param state 算法与阵元类型：1=GA_单 2=PSO_单 3=SA_单 4=Tabu_单
     *              5=GA_偶 6=PSO_偶 7=SA_偶 8=Tabu_偶
     */
    @GetMapping("/doublet")
    public BaseResponse<JsonNode> doublet(@RequestParam String nu,
                                          @RequestParam String value1,
                                          @RequestParam(required = false, defaultValue = "") String value2,
                                          @RequestParam String state) {
        try {
            return ResultUtils.success(
                    pythonEmulationService.doublet(nu, value1, value2, state), "仿真成功");
        } catch (Exception e) {
            log.error("双极子仿真失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 单极子天线仿真（旋转方向图）。
     */
    @GetMapping("/monopole")
    public BaseResponse<JsonNode> monopole(@RequestParam String nu,
                                           @RequestParam String value1,
                                           @RequestParam(required = false, defaultValue = "") String value2) {
        try {
            return ResultUtils.success(
                    pythonEmulationService.monopole(nu, value1, value2), "仿真成功");
        } catch (Exception e) {
            log.error("单极子仿真失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 综合仿真：同一组参数依次运行 GA / PSO / SA / Tabu 并对比。
     *
     * @param nu     阵元数
     * @param value1 目标方向角（度）
     * @param kind   阵元类型：sp=单阵元（默认），dp=偶阵元
     */
    @GetMapping("/emulation")
    public BaseResponse<JsonNode> emulation(
            @RequestParam String nu,
            @RequestParam String value1,
            @RequestParam(required = false, defaultValue = "sp") String kind) {
        try {
            return ResultUtils.success(
                    pythonEmulationService.emulation(nu, value1, kind), "仿真成功");
        } catch (Exception e) {
            log.error("综合仿真失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 入射波 / 反射仿真。
     */
    @GetMapping("/ybfs")
    public BaseResponse<JsonNode> ybfs(@RequestParam String nu,
                                       @RequestParam String value1,
                                       @RequestParam(required = false, defaultValue = "") String value2) {
        try {
            return ResultUtils.success(
                    pythonEmulationService.ybfs(nu, value1, value2), "仿真成功");
        } catch (Exception e) {
            log.error("入射波仿真失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 全折射仿真，返回两个结果值。
     */
    @GetMapping("/qzs")
    public BaseResponse<JsonNode> qzs(@RequestParam String nu,
                                      @RequestParam String value1) {
        try {
            return ResultUtils.success(
                    pythonEmulationService.qzs(nu, value1), "仿真成功");
        } catch (Exception e) {
            log.error("全折射仿真失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 全反射仿真。
     */
    @GetMapping("/qfs")
    public BaseResponse<JsonNode> qfs(@RequestParam String nu) {
        try {
            return ResultUtils.success(
                    pythonEmulationService.qfs(nu), "仿真成功");
        } catch (Exception e) {
            log.error("全反射仿真失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 高斯波束仿真。
     */
    @GetMapping("/gs")
    public BaseResponse<JsonNode> gs(@RequestParam String nu,
                                     @RequestParam String value1,
                                     @RequestParam(required = false, defaultValue = "") String value2) {
        try {
            return ResultUtils.success(
                    pythonEmulationService.gs(nu, value1, value2), "仿真成功");
        } catch (Exception e) {
            log.error("高斯仿真失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 正弦 EH 场仿真。
     */
    @GetMapping("/zxb")
    public BaseResponse<JsonNode> zxb(@RequestParam String value1,
                                      @RequestParam String value2) {
        try {
            return ResultUtils.success(
                    pythonEmulationService.zxb(value1, value2), "仿真成功");
        } catch (Exception e) {
            log.error("正弦 EH 场仿真失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }
}
