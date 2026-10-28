package com.hyao.mybatis.controller;

import com.hyao.mybatis.model.dict.DictItemVO;
import com.hyao.mybatis.model.dict.DictQueryRequest;
import com.hyao.mybatis.model.dict.DictUpdateNameRequest;
import com.hyao.mybatis.service.DictService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 数据字典相关控制器。
 */
@RestController
@RequestMapping("/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    /**
     * 查询数据字典。
     * <pre>
     *     请求示例：
     *     POST /dict/updateName
     *
     *     Content-Type: application/json
     *     { "dictType": "order_status" }
     * </pre>
     *
     * @param req 查询请求
     * @return 数据字典结果列表
     */
    @PostMapping("/list")
    public List<DictItemVO> list(@Valid @RequestBody DictQueryRequest req) {
        return dictService.queryByType(req);
    }

    /**
     * 更新对应数据字典的显示名。
     * <pre>
     *     请求示例：
     *     POST /dict/updateName
     *
     *     Content-Type: application/json
     *     { "dictType": "order_status", "dictCode": "1", "dictName": "已支付" }
     * </pre>
     *
     * @param req 更新请求
     */
    @PostMapping("/updateName")
    public void updateName(@Valid @RequestBody DictUpdateNameRequest req) {
        dictService.updateName(req);
    }
}
