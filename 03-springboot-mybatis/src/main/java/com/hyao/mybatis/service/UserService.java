package com.hyao.mybatis.service;

import com.hyao.mybatis.common.enums.UserStatus;
import com.hyao.mybatis.entity.User;
import com.hyao.mybatis.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户服务类.
 */
@Service("userService")
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 根据用户昵称查询用户
     *
     * @param nickname 用户昵称
     * @return 用户信息
     */
    public Map<String, Object> queryByNickname(String nickname) {
        User user = userMapper.selectByNickname(nickname);
        if (user == null) {
            return null;
        }
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("nickname", user.getNickname());
        userMap.put("avatar", user.getAvatar());
        userMap.put("status", user.getStatus());
        userMap.put("createAt", user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        return userMap;
    }

    /**
     * 插入用户
     *
     * @param params 用户信息
     * @return 受影响行数
     */
    public int insertUser(Map<String, Object> params) {
        User user = new User();
        // 生成id...
        user.setId("u0000001");
        user.setNickname((String) params.get("nickname"));
        user.setAvatar((String) params.get("avatar"));
        user.setStatus(UserStatus.DISABLE);
        return userMapper.insert(user);
    }

    /**
     * 根据id更新昵称
     *
     * @param id       用户id
     * @param nickname 新昵称
     * @return 受影响行数
     */
    public int updateNickname(String id, String nickname) {
        // 校验入参
        return userMapper.updateNicknameById(id, nickname);
    }

    /**
     * 根据id删除用户. 逻辑删除.
     *
     * @param id 用户id
     * @return 受影响行数
     */
    public int deleteUser(String id) {
        // 校验入参
        return userMapper.deleteById(id);
    }
}
