package com.hyao.base.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * MVC视图控制器示例。主要用于页面渲染。
 * <li>
 *     {@code @Controller}注解的类，用于定义MVC控制器类。该类中的方法返回的字符
 *     串表示视图名称，不会直接写入响应体，而是通过视图解析器处理后返回给客户端。
 * </li>
 * <li>
 *     {@code @RequestMapping}注解在类上，表示该Controller中所有方法的公共
 *     路径前缀。类级 @RequestMapping+ 方法级 @RequestMapping = 完整路径。
 *     比如第一个方法的路径是 /hello/redirect
 * </li>
 */
@Controller
@RequestMapping("/hello")
public class MyController {

    /**
     * 重定向到其他controller或资源。
     * <p>302重定向，浏览器地址会变化。
     *
     * <p>{@code @RequestMapping}注解在方法上，指定该方法对应的请求路径和请求方法。
     */
    @RequestMapping(path = "/redirect", method = RequestMethod.GET)
    public String helloByRedirect() {
        return "redirect:/index.html";
    }

    /**
     * 转发到其他controller或资源。
     * <p>服务器内部转发，浏览器地址不变。
     */
    @RequestMapping("/forward")
    public String helloByForward() {
        return "forward:/index.html";
    }

    /**
     * 通过视图解析器渲染到index资源。
     * <p>注意，由于没有引入模板引擎，这里会走SpringMVC默认的视图解析器
     * {@code InternalResourceViewResolver}，他会把视图名拼接成转
     * 发路径，
     * <pre>
     *     prefix + "index" + suffix
     * </pre>
     * 这里的prefix和suffix是application.yaml中的配置：
     * <pre>
     *     spring.mvc.view.prefix
     *     spring.mvc.view.suffix
     * </pre>
     * 模版引擎如thymeleaf在另外的模块中学习。
     * @return 视图名称
     */
    @RequestMapping("/view")
    public String helloByView() {
        return "index";
    }
}
