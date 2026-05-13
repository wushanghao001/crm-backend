package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crm.entity.UserSession;
import com.example.crm.mapper.UserSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private final UserSessionMapper userSessionMapper;

    public SessionService(UserSessionMapper userSessionMapper) {
        this.userSessionMapper = userSessionMapper;
    }

    @Transactional
    public UserSession createSession(Long userId, String loginIp, String deviceInfo) {
        deleteUserSessions(userId);

        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setSessionToken(UUID.randomUUID().toString().replace("-", ""));
        session.setLoginIp(loginIp);
        session.setDeviceInfo(deviceInfo);
        session.setLoginTime(LocalDateTime.now());
        session.setLastAccessTime(LocalDateTime.now());
        session.setStatus(1);

        userSessionMapper.insert(session);
        return session;
    }

    @Transactional
    public UserSession createSessionWithToken(Long userId, String sessionToken, String loginIp, String deviceInfo) {
        deleteUserSessions(userId);

        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setSessionToken(sessionToken);
        session.setLoginIp(loginIp);
        session.setDeviceInfo(deviceInfo);
        session.setLoginTime(LocalDateTime.now());
        session.setLastAccessTime(LocalDateTime.now());
        session.setStatus(1);

        userSessionMapper.insert(session);
        return session;
    }

    @Transactional
    public void deleteUserSessions(Long userId) {
        userSessionMapper.deleteUserSessions(userId);
    }

    @Transactional
    public void deleteSessionByToken(String token) {
        userSessionMapper.deleteSessionByToken(token);
    }

    public UserSession getActiveSession(Long userId) {
        return userSessionMapper.findActiveSessionByUserId(userId);
    }

    public UserSession getActiveSessionByToken(String token) {
        return userSessionMapper.findActiveSessionByToken(token);
    }

    public void updateLastAccessTime(String token) {
        UserSession session = userSessionMapper.findActiveSessionByToken(token);
        if (session != null) {
            session.setLastAccessTime(LocalDateTime.now());
            userSessionMapper.updateById(session);
        }
    }

    public boolean validateSession(String token, Long userId) {
        UserSession session = userSessionMapper.findActiveSessionByToken(token);
        return session != null && session.getUserId().equals(userId);
    }

    public List<UserSession> getUserSessions(Long userId) {
        LambdaQueryWrapper<UserSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSession::getUserId, userId)
                   .eq(UserSession::getStatus, 1)
                   .orderByDesc(UserSession::getLastAccessTime);
        return userSessionMapper.selectList(queryWrapper);
    }

    public List<UserSession> getAllActiveSessions() {
        LambdaQueryWrapper<UserSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSession::getStatus, 1)
                   .orderByDesc(UserSession::getLastAccessTime);
        return userSessionMapper.selectList(queryWrapper);
    }
}
