package com.example.crm.aspect;

import com.example.crm.annotation.OperationLog;
import com.example.crm.entity.User;
import com.example.crm.mapper.OperationLogMapper;
import com.example.crm.security.JwtAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class OperationLogAspect {

    private final OperationLogMapper logMapper;

    public OperationLogAspect(OperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Around("@annotation(com.example.crm.annotation.OperationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        com.example.crm.annotation.OperationLog annotation = signature.getMethod().getAnnotation(com.example.crm.annotation.OperationLog.class);

        String operator = "未知用户";
        Long operatorId = null;

        Object authObj = SecurityContextHolder.getContext().getAuthentication();
        if (authObj != null && !authObj.getClass().getName().contains("AnonymousAuthenticationToken")) {
            if (authObj instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken token = (JwtAuthenticationToken) authObj;
                Object principal = token.getPrincipal();
                if (principal instanceof User) {
                    User user = (User) principal;
                    operator = user.getUsername();
                    operatorId = user.getId();
                }
            }
        }

        HttpServletRequest request = getRequest();
        String ip = request != null ? request.getRemoteAddr() : "";
        String params = getParams(joinPoint);

        com.example.crm.entity.OperationLog logEntry = new com.example.crm.entity.OperationLog();
        logEntry.setModule(annotation.module());
        logEntry.setType(annotation.type());
        logEntry.setContent(annotation.content());
        logEntry.setOperator(operator);
        logEntry.setOperatorId(operatorId != null ? String.valueOf(operatorId) : "0");
        logEntry.setIp(ip);
        logEntry.setParams(params);
        logEntry.setCreatedAt(LocalDateTime.now());

        Object result = null;
        try {
            result = joinPoint.proceed();
            logEntry.setStatus(1);
            logEntry.setResult("成功");
            return result;
        } catch (Exception e) {
            logEntry.setStatus(0);
            logEntry.setResult("失败");
            logEntry.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            try {
                logMapper.insert(logEntry);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getParams(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return "";
        }
        Map<String, Object> params = new HashMap<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        for (int i = 0; i < args.length; i++) {
            if (i < paramNames.length) {
                Object arg = args[i];
                if (arg != null && !isFileOrStream(arg)) {
                    params.put(paramNames[i], arg.toString());
                }
            }
        }
        return params.toString();
    }

    private boolean isFileOrStream(Object obj) {
        return obj instanceof java.io.InputStream
            || obj instanceof java.io.OutputStream
            || obj instanceof java.io.File;
    }
}