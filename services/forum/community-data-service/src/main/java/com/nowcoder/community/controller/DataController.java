package com.nowcoder.community.controller;

import com.nowcoder.community.service.DataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

/**
 * 后台统计页面控制器。
 */
@Controller
@RequestMapping("/data")
public class DataController {

    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * 进入统计页面。
     *
     * @return 统计页面模板
     */
    @RequestMapping
    public String getDataPage() {
        return "/site/admin/data";
    }

    /**
     * 统计指定时间范围内的 UV。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @param model 页面模型
     * @return 转发到统计页面
     */
    @PostMapping("/uv")
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end,
                        Model model) {
        long uv = dataService.calculateUV(start, end);
        model.addAttribute("uvResult", uv);
        model.addAttribute("uvStartDate", start);
        model.addAttribute("uvEndDate", end);
        return "forward:/data";
    }

    /**
     * 统计指定时间范围内的 DAU。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @param model 页面模型
     * @return 转发到统计页面
     */
    @PostMapping("/dau")
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                         @DateTimeFormat(pattern = "yyyy-MM-dd") Date end,
                         Model model) {
        long dau = dataService.calculateDAU(start, end);
        model.addAttribute("dauResult", dau);
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);
        return "forward:/data";
    }
}
