package com.quiz.service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.quiz.model.vo.LoginUserVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 用户服务测试
 *
 * @author shengjie fan
 */
@SpringBootTest
public class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    void userRegister() {
        String userAccount = "user2";
        String username = "张三";
        String userProfile = "这是张三";
        String userPassword = "12345678";
        String checkPassword = "12345678";
        try {
            long result = userService.userRegister(userAccount, username, userProfile, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "AdminFan";
            result = userService.userRegister(userAccount, username, userProfile, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userPassword = null;
            result = userService.userRegister(userAccount, username, userProfile, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            checkPassword = "123456";
            result = userService.userRegister(userAccount, username, userProfile, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
        } catch (Exception e) {
        }
    }
}