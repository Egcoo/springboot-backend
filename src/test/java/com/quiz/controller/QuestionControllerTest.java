package com.quiz.controller;

import com.quiz.common.BaseResponse;
import com.quiz.model.entity.User;
import com.quiz.model.vo.QuestionVO;
import com.quiz.service.QuestionService;
import com.quiz.utils.CollaborativeFilteringTool;
import com.quiz.utils.SimpleRateLimiter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Egcoo
 * @date 2025/5/20 - 11:06
 */
class QuestionControllerTest {
    @Mock
    private QuestionService questionService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private QuestionController questionController;

    private List<QuestionVO> questionVOList;

    @BeforeEach
    void setUp() {
        questionVOList = new ArrayList<>();
        // 添加测试数据
        for (int i = 0; i < 15; i++) {
            QuestionVO questionVO = new QuestionVO();
            questionVO.setId((long) i);
            questionVO.setTitle("Question " + i);
            questionVO.setContent("Content " + i);
            questionVOList.add(questionVO);
        }
    }

    @AfterEach
    void tearDown() {
        questionVOList = null;
    }

    @Test
    void testRecommendForDifferentUsers() {
         // 创建用户1的会话信息
        User user1 = new User();
        user1.setId(1L);
        // 保存用户1的会话属性
        when(request.getSession().getAttribute("USER_LOGIN_STATE")).thenReturn(user1);

        // 为目标用户1生成推荐
        BaseResponse<List<QuestionVO>> response1 = questionController.recommend(request);
        List<QuestionVO> recommendationsUser1 = response1.getData();

        // 创建用户2的会话信息
        User user2 = new User();
        user2.setId(2L);
        // 保存用户2的会话属性
        when(request.getSession().getAttribute("USER_LOGIN_STATE")).thenReturn(user2);

        // 为目标用户2生成推荐
        BaseResponse<List<QuestionVO>> response2 = questionController.recommend(request);
        List<QuestionVO> recommendationsUser2 = response2.getData();

        // 检查两个用户的推荐结果是否有差异
        Set<String> user1Tags = recommendationsUser1.stream().map(QuestionVO::getTags).collect(Collectors.toSet());
        Set<String> user2Tags = recommendationsUser2.stream().map(QuestionVO::getTags).collect(Collectors.toSet());
        assertFalse(user1Tags.equals(user2Tags), "两个用户的推荐结果应包含不同类别的题目");

        // 验证推荐结果是否来自测试数据
        for (QuestionVO questionVO : recommendationsUser1) {
            assertTrue(questionVOList.contains(questionVO), "推荐结果应来自测试数据");
        }
        for (QuestionVO questionVO : recommendationsUser2) {
            assertTrue(questionVOList.contains(questionVO), "推荐结果应来自测试数据");
        }

        // 验证questionService.list()方法是否被正确调用
        verify(questionService, times(2)).list();
    }

}