package com.hyao.mybatis.service;

import com.hyao.mybatis.common.enums.Gender;
import com.hyao.mybatis.common.enums.UserStatus;
import com.hyao.mybatis.entity.User;
import com.hyao.mybatis.entity.UserProfiles;
import com.hyao.mybatis.mapper.UserMapper;
import com.hyao.mybatis.mapper.UserProfilesMapper;
import com.hyao.mybatis.model.UserPageQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务类.
 */
@Service("userService")
public class UserService {

    private final UserMapper userMapper;

    private final UserProfilesMapper userProfilesMapper;

    public UserService(UserMapper userMapper, UserProfilesMapper userProfilesMapper) {
        this.userMapper = userMapper;
        this.userProfilesMapper = userProfilesMapper;
    }

    /**
     * 根据用户id查询用户
     *
     * @param id 用户id
     * @return 用户信息
     */
    public Map<String, Object> queryUserById(String id) {
        User user = userMapper.queryUserById(id);
        if (user == null) {
            return null;
        }
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("nickname", user.getNickname());
        userMap.put("avatar", user.getAvatar());
        userMap.put("status", user.getStatus());
        userMap.put("createAt", user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        return userMap;
    }

    /**
     * 按条件查询用户. (这种分页逻辑传参要求前端遵守分页传参约定.)
     *
     * <p>参数由 00 模块的 Bean Validation 校验（Controller 上的 {@code @Valid}），这里不再手写校验。
     *
     * @param query 查询条件
     * @return 分页查询结果，包含分页参数
     */
    public Map<String, Object> pageQueryUserConditional(UserPageQuery query) {
        int pageSize = query.getPageSize();
        Map<String, Object> params = new HashMap<>();
        // 多查询一条,作为下一页的起始项id
        params.put("pageSize", pageSize + 1);
        params.put("nextCursor", query.getNextCursor());
        params.put("status", query.getStatus());
        params.put("gender", query.getGender());
        params.put("beginDate", query.getBeginDate());
        params.put("endDate", query.getEndDate());
        params.put("province", query.getProvince());
        params.put("city", query.getCity());

        List<Map<String, Object>> list = userMapper.pageQueryUserConditional(params);
        // 分页参数
        boolean hasNextPage = false;
        String nextCursor = null;
        if (list.size() == pageSize + 1) {
            hasNextPage = true;
            // 移除多查出来的最后一个元素,作为下一页的起始项id
            Map<String, Object> nextPageStart = list.remove(list.size() - 1);
            nextCursor = (String) nextPageStart.get("id");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("hasNextPage", hasNextPage);
        result.put("nextCursor", nextCursor);
        result.put("resultData", list);
        return result;
    }

    /**
     * 插入用户
     *
     * @param params 用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void insertUser(Map<String, Object> params) {
        User user = new User();
        // 生成id...
        user.setId("u0000001");
        user.setNickname((String) params.get("nickname"));
        user.setAvatar((String) params.get("avatar"));
        user.setStatus(UserStatus.DISABLED);

        UserProfiles userProfiles = new UserProfiles();
        userProfiles.setUserId(user.getId());
        userProfiles.setRealName((String) params.get("realName"));
        Integer genderCode = (Integer) params.get("gender");
        if (genderCode != null) {
            userProfiles.setGender(Gender.of(genderCode));
        } else {
            userProfiles.setGender(Gender.UNKNOWN);
        }
        String birthday = (String) params.get("birthday");
        if (birthday != null) {
            userProfiles.setBirthday(LocalDate.parse(birthday, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        userProfiles.setProvince((String) params.get("province"));
        userProfiles.setCity((String) params.get("city"));
        userProfiles.setAddress((String) params.get("address"));
        userProfiles.setCompany((String) params.get("company"));
        userProfiles.setBio((String) params.get("bio"));

        userMapper.insertUser(user);
        userProfilesMapper.insertUserProfiles(userProfiles);
    }

    /**
     * 根据id更新
     *
     * @param params 用户信息
     * @return 受影响行数
     */
    public int updateById(Map<String, Object> params) {
        // 校验入参...
        String id = (String) params.get("id");
        String nickname = (String) params.get("nickname");
        String avatar = (String) params.get("avatar");
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        return userMapper.updateUserSelective(user);
    }

    /**
     * 根据id删除用户. 逻辑删除.
     *
     * @param id 用户id
     * @return 受影响行数
     */
    public int deleteUser(String id) {
        // 校验入参
        User user = new User();
        user.setId(id);
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());
        return userMapper.updateUserSelective(user);
    }
}
