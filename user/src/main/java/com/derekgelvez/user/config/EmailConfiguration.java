package com.derekgelvez.user.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * SMTP client used by {@code EmailService} to send verification and password-reset emails through
 * Gmail. Host, port and TLS are set here in code; because this bean replaces Spring Boot's
 * auto-configured mail sender, only {@code spring.mail.username} and {@code spring.mail.password}
 * are read from configuration.
 */
@Configuration
public class EmailConfiguration {

    @Value("${spring.mail.username}")
    private String emailUsername;

    @Value("${spring.mail.password}")
    private String password;

    @Bean
    public JavaMailSender javamailsender(){
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(emailUsername);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return mailSender;
    }
}
