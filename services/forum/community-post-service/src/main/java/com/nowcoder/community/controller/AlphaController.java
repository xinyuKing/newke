package com.nowcoder.community.controller;

import com.nowcoder.community.service.AlphaService;
import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Alpha 示例控制器。
 *
 * <p>该控制器主要用于演示 Spring MVC 的常见能力，包括参数接收、页面渲染、Cookie、Session 和 Ajax。</p>
 */
@Controller
@RequestMapping("/alpha")
public class AlphaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlphaController.class);

    private final AlphaService alphaService;

    public AlphaController(AlphaService alphaService) {
        this.alphaService = alphaService;
    }

    @GetMapping("/hello")
    @ResponseBody
    public String sayHello() {
        return "Hello Spring Boot";
    }

    @GetMapping("/data")
    @ResponseBody
    public String getData() {
        return alphaService.find();
    }

    /**
     * 演示如何直接读写原生 HTTP 请求与响应。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     */
    @RequestMapping("/http")
    public void http(HttpServletRequest request, HttpServletResponse response) {
        LOGGER.info("request method={}, path={}", request.getMethod(), request.getServletPath());
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            LOGGER.debug("header {}={}", name, request.getHeader(name));
        }
        LOGGER.debug("query code={}", request.getParameter("code"));

        response.setContentType("text/html;charset=utf-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write("<h1>study</h1>");
        } catch (IOException ex) {
            LOGGER.error("write response failed", ex);
        }
    }

    /**
     * GET 请求参数示例。
     *
     * @param current 当前页
     * @param limit 每页条数
     * @return 固定示例文案
     */
    @GetMapping("/students")
    @ResponseBody
    public String getStudents(@RequestParam(name = "current", defaultValue = "1") int current,
                              @RequestParam(name = "limit", defaultValue = "10") int limit) {
        LOGGER.debug("query students, current={}, limit={}", current, limit);
        return "some students";
    }

    /**
     * 路径变量示例。
     *
     * @param id 学生 ID
     * @return 固定示例文案
     */
    @GetMapping("/student/{id}")
    @ResponseBody
    public String getStudent(@PathVariable("id") int id) {
        LOGGER.debug("query student, id={}", id);
        return "a student";
    }

    /**
     * POST 表单示例。
     *
     * @param name 姓名
     * @param age 年龄
     * @return 固定示例文案
     */
    @PostMapping("/student")
    @ResponseBody
    public String saveStudent(String name, int age) {
        LOGGER.debug("save student, name={}, age={}", name, age);
        return "success";
    }

    /**
     * 通过 {@link ModelAndView} 返回页面。
     *
     * @return 页面视图
     */
    @GetMapping("/teacher")
    public ModelAndView getTeacher() {
        ModelAndView mav = new ModelAndView();
        mav.addObject("name", "tom1");
        mav.addObject("age", "30");
        mav.setViewName("/demo/view");
        return mav;
    }

    /**
     * 通过 {@link Model} 返回页面。
     *
     * @param model 页面模型
     * @return 视图名称
     */
    @GetMapping("/school")
    public String getSchool(Model model) {
        model.addAttribute("name", "CUG");
        model.addAttribute("age", "71");
        return "/demo/view";
    }

    /**
     * 返回单个 JSON 对象。
     *
     * @return 员工信息
     */
    @GetMapping("/emp")
    @ResponseBody
    public Map<String, Object> getEmp() {
        Map<String, Object> emp = new HashMap<>();
        emp.put("name", "tom");
        emp.put("age", 30);
        emp.put("salary", 30000);
        return emp;
    }

    /**
     * 返回 JSON 数组。
     *
     * @return 员工列表
     */
    @GetMapping("/emps")
    @ResponseBody
    public List<Map<String, Object>> getEmps() {
        List<Map<String, Object>> emps = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Map<String, Object> emp = new HashMap<>();
            emp.put("name", "tom" + i);
            emp.put("age", 30 + i);
            emp.put("salary", 30000 + i);
            emps.add(emp);
        }
        return emps;
    }

    /**
     * Cookie 设置示例。
     *
     * @param response HTTP 响应
     * @return 处理结果
     */
    @GetMapping("/cookie/set")
    @ResponseBody
    public String setCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("code", CommunityUtil.generateUUID());
        cookie.setPath("/community/alpha");
        cookie.setMaxAge(60 * 10);
        response.addCookie(cookie);
        return "set cookie";
    }

    /**
     * Cookie 读取示例。
     *
     * @param code Cookie 中的 code
     * @return 处理结果
     */
    @GetMapping("/cookie/get")
    @ResponseBody
    public String getCookie(@CookieValue(value = "code", required = false) String code) {
        LOGGER.debug("cookie code={}", code);
        return "get cookie";
    }

    /**
     * Session 设置示例。
     *
     * @param session 当前会话
     * @return 处理结果
     */
    @GetMapping("/session/set")
    @ResponseBody
    public String setSession(HttpSession session) {
        session.setAttribute("id", 1);
        session.setAttribute("name", "Test");
        return "set session";
    }

    /**
     * Session 读取示例。
     *
     * @param session 当前会话
     * @return 处理结果
     */
    @GetMapping("/session/get")
    @ResponseBody
    public String getSession(HttpSession session) {
        LOGGER.debug("session id={}, name={}", session.getAttribute("id"), session.getAttribute("name"));
        return "get session";
    }

    /**
     * Ajax 请求示例。
     *
     * @param name 姓名
     * @param age 年龄
     * @return JSON 字符串
     */
    @PostMapping("/ajax")
    @ResponseBody
    public String testAjax(String name, int age) {
        LOGGER.debug("ajax request, name={}, age={}", name, age);
        return CommunityUtil.getJSONString(0, "操作成功");
    }
}
