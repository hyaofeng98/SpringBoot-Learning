package com.hyao.validation.programmatic;

import com.hyao.common.exception.BizException;
import com.hyao.common.exception.SysErrorCode;
import com.hyao.validation.custom.dto.CustomDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProgrammaticService {

    private final Validator validator;

    public ProgrammaticService(Validator validator) {
        this.validator = validator;
    }

    /**
     * 演示在请求校验之外的其他地方手动校验.
     */
    public void validate(CustomDTO dto) {
        Set<ConstraintViolation<CustomDTO>> validations = validator.validate(dto);
        if (!CollectionUtils.isEmpty(validations)) {
            String msg = validations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            log.error("[{}] {}", SysErrorCode.PARAM_INVALID.getCode(), msg);
            throw new BizException(SysErrorCode.PARAM_INVALID);
        }
    }
}
