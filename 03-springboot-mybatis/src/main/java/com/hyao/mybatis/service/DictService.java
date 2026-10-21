package com.hyao.mybatis.service;

import com.hyao.mybatis.entity.dict.DictItem;
import com.hyao.mybatis.mapper.dict.DictItemMapper;
import com.hyao.mybatis.model.dict.DictItemVO;
import com.hyao.mybatis.model.dict.DictQueryRequest;
import com.hyao.mybatis.model.dict.DictUpdateNameRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据字典服务.
 */
@Service
public class DictService {

    private final DictItemMapper dictItemMapper;

    public DictService(DictItemMapper dictItemMapper) {
        this.dictItemMapper = dictItemMapper;
    }

    /**
     * 根据字典类型查询数据字典.
     *
     * @param req 查询请求
     * @return 数据字典列表
     */
    public List<DictItemVO> queryByType(DictQueryRequest req) {
        List<DictItem> dictItems = dictItemMapper.queryByType(req.getDictType());
        List<DictItemVO> result = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dictItems)) {
            for (DictItem dictItem : dictItems) {
                result.add(buildDictItemVO(dictItem));
            }
        }
        return result;
    }

    /**
     * 更新字典编码显示名
     *
     * @param req 更新字典编码显示名请求
     */
    public void updateName(DictUpdateNameRequest req) {
        dictItemMapper.updateName(req.getDictType(), req.getDictCode(), req.getDictName());
    }

    private DictItemVO buildDictItemVO(DictItem dictItem) {
        DictItemVO vo = new DictItemVO();
        vo.setDictType(dictItem.getDictType());
        vo.setDictCode(dictItem.getDictCode());
        vo.setDictName(dictItem.getDictName());
        vo.setSort(dictItem.getSort());
        vo.setStatus(dictItem.getStatus());
        return vo;
    }

}
