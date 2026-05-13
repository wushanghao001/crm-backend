package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.UserSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {

    @Select("SELECT * FROM user_session WHERE user_id = #{userId} AND status = 1 ORDER BY last_access_time DESC LIMIT 1")
    UserSession findActiveSessionByUserId(Long userId);

    @Select("SELECT * FROM user_session WHERE session_token = #{sessionToken} AND status = 1")
    UserSession findActiveSessionByToken(String sessionToken);

    @Delete("DELETE FROM user_session WHERE user_id = #{userId}")
    void deleteUserSessions(Long userId);

    @Delete("DELETE FROM user_session WHERE session_token = #{sessionToken}")
    void deleteSessionByToken(String sessionToken);
}
