package com.example.crm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("CRM系统 <764124654@qq.com>");
        message.setTo(to);
        message.setSubject("【CRM系统】您的验证码");
        message.setText(buildEmailContent(code));
        mailSender.send(message);
    }

    private String buildEmailContent(String code) {
        return """
                您好！

                您的验证码是：%s

                验证码有效期为5分钟，请勿泄露给他人。

                如果您没有请求此验证码，请忽略此邮件。

                ---
                CRM客户管理系统
                """.formatted(code);
    }
}
