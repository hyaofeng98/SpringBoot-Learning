package com.hyao.validation.hibernate;

import com.hyao.common.model.Result;
import com.hyao.validation.hibernate.dto.HibernateDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Hibernate Validator 示例.
 *
 * @see HibernateDTO
 */
@RestController
@RequestMapping("/valid")
public class HibernateValidatorController {

    /**
     * <pre>
     *     请求示例:
     *     POST /valid/hibernate
     *
     *     Content-Type: application/json
     *     { "date":"2023-01-01", "l": "abcdefghijk", "r": 11, "url": "http://www.example.com/a", "creditCard": "1234567890123456", "isbn": "0306406153" }
     * </pre>
     */
    @PostMapping("/hibernate")
    public Result<Void> validate(@Valid @RequestBody HibernateDTO hibernateDTO) {
        return Result.ok();
    }

}
