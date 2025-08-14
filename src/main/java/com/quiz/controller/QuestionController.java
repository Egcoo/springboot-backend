package com.quiz.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quiz.annotation.AuthCheck;
import com.quiz.common.BaseResponse;
import com.quiz.common.DeleteRequest;
import com.quiz.common.ErrorCode;
import com.quiz.common.ResultUtils;
import com.quiz.constant.UserConstant;
import com.quiz.exception.BusinessException;
import com.quiz.exception.ThrowUtils;
import com.quiz.mapper.QuestionMapper;
import com.quiz.model.dto.question.*;
import com.quiz.model.entity.Question;
import com.quiz.model.entity.User;
import com.quiz.model.vo.QuestionVO;
import com.quiz.sentinel.SentinelConstant;
import com.quiz.service.QuestionService;
import com.quiz.service.UserService;
import com.quiz.utils.CollaborativeFilteringTool;
import com.quiz.utils.SimpleRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import static com.quiz.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 题目接口
 *
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;



    // region 增删改查

    /**
     * 创建题目
     *
     * 接口调用的是questionAdd对象，但是实际上写数据库的时候是question对象
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 数据校验
        questionService.validQuestion(question, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 数据校验
        questionService.validQuestion(question, false);
        // 判断是否存在
        long id = questionUpdateRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题目（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取题目列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long size = questionQueryRequest.getPageSize();
        User loginUserPermitNull = userService.getLoginUserPermitNull(request);

        System.err.println(":" + loginUserPermitNull);
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);

        Page<QuestionVO> questionVOPage = questionService.getQuestionVOPage(questionPage, request);
        // 获取封装类
        return ResultUtils.success(questionVOPage);
    }

    private static final SimpleRateLimiter rateLimiter = new SimpleRateLimiter(1, 1000); // 最多5个请求/秒

    @Autowired
    QuestionMapper questionMapper;
    @GetMapping("/recommend")
//    @SaIgnore
    public BaseResponse<List<QuestionVO>> recommend(
                                                    HttpServletRequest request) {
//        Object o = StpUtil.getSession().get(USER_LOGIN_STATE);
        if (!rateLimiter.tryAcquire()) {
//            return ResultUtils.success(new ArrayList<>()); // HTTP 429 Too Many Requests
        }

        Object o1 = request.getSession().getAttribute(USER_LOGIN_STATE);
        // 获取所有题目
        List<Question> questionList = questionService.list();



        if (o1 != null) {
            User loginUser = (User)o1;
//            final User loginUser = userService.getLoginUser(request);
            // 创建工具类实例
            CollaborativeFilteringTool cfTool = new CollaborativeFilteringTool();
            Map<String, Long> userTagMap = new HashMap<>();
            for (Question question : questionList) {
                String tags = question.getTags();
                Long userId = question.getUserId();
                if (StringUtils.isNotBlank(tags)) {
                    String[] split = tags.split(",");
                    for (String tag : split) {
                        String replace = tag.replace("[", "");
                        tag = replace.replace("]", "");
                        // 添加到用户标签映射中
                        if (!userTagMap.containsKey(tag)) {
                            userTagMap.put(tag, userId);
                            cfTool.addUserItem(userId, userTagMap.get(tag));
                        } else {
                            // 生成一个随机数
                            long l = System.currentTimeMillis();
                            userTagMap.put(tag, l);
                            cfTool.addUserItem(userId, l);
                        }

                    }
                }

            }

//            for (Question question : questionList) {
//                cfTool.addUserItem(question.getUserId(), question.getId());
//            }

            // 为用户1生成推荐
            long targetUserId = loginUser.getId();
            int topN = 8;

            // 获取相似用户及其相似度
            Map<Long, Double> similarUsers = cfTool.getSimilarUsersWithSimilarity(targetUserId, topN);
            System.out.println("相似用户及其相似度: " + similarUsers);

            // 生成推荐及其推荐分数
            Map<Long, Double> recommendations = cfTool.recommendItemsWithScores(targetUserId, topN);
            Set<Long> integers = recommendations.keySet();
            System.out.println("为用户" + targetUserId + "推荐的物品: " + integers);
            System.out.println("为用户" + targetUserId + "推荐的物品及其推荐分数:");
            //System.out.println("为未登录用户推荐的物品: " + integers);
            //System.out.println("为未登录用户推荐的物品及其推荐分数:");
            //System.out.println("物品 1 的推荐分数: 0.764902803707298");
            //System.out.println("物品 2 的推荐分数: 0.596706306468475");
            //System.out.println("物品 5 的推荐分数: 0.395930401834572");
            for (Map.Entry<Long, Double> entry : recommendations.entrySet()) {
                System.out.println("物品 " + entry.getKey() + " 的推荐分数: " + entry.getValue());
            }


            // 清空数据
            cfTool.clearData();
            System.out.println("数据已清空，当前用户-物品数据: " + cfTool.getUserItemMap());

            List<String> tags = new ArrayList<>();
            if (integers.size() > 0) {
                System.out.println("推荐成功");
                for (String s : userTagMap.keySet()) {
                    if (integers.contains(userTagMap.get(s))) {
                        tags.add(s);
                    }
                }

                if (tags.size() > 0) {
                    System.out.println("为用户" + targetUserId + "推荐的分类: " + tags);
                    List<String> conditions = tags.stream()
                            .map(tag -> "JSON_CONTAINS(tags, '" + tag + "')")
                            .collect(Collectors.toList());
                    QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
                    queryWrapper.apply( String.join(" OR ", conditions) );
                    List<Question> questions = questionService.list(queryWrapper);
                    List<QuestionVO> questionVOList = questions.stream().map(question -> {
                        return QuestionVO.objToVo(question);
                    }).collect(Collectors.toList());
                    return ResultUtils.success(questionVOList);
                } else {
                    Collections.shuffle(questionList);
                    List<Question> questions = questionList.size() > 8 ? questionList.subList(0, 8) : questionList;
                    List<QuestionVO> questionVOList = questions.stream().map(question -> {
                        return QuestionVO.objToVo(question);
                    }).collect(Collectors.toList());
                    return ResultUtils.success(questionVOList);
                }
            } else {
                Collections.shuffle(questionList);
                List<Question> questions = questionList.size() > 8 ? questionList.subList(0, 8) : questionList;
                List<QuestionVO> questionVOList = questions.stream().map(question -> {
                    return QuestionVO.objToVo(question);
                }).collect(Collectors.toList());
                return ResultUtils.success(questionVOList);
            }
        } else {
            Collections.shuffle(questionList);
            List<Question> questions = questionList.size() > 8 ? questionList.subList(0, 8) : questionList;
            List<QuestionVO> questionVOList = questions.stream().map(question -> {
                return QuestionVO.objToVo(question);
            }).collect(Collectors.toList());
            return ResultUtils.success(questionVOList);
        }
    }

    /**
     * 分页获取题目列表（封装类 - 限流版）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo/sentinel")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPageSentinel(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 基于 IP 限流
        String remoteAddr = request.getRemoteAddr();
        Entry entry = null;
        try {
            entry = SphU.entry(SentinelConstant.listQuestionVOByPage, EntryType.IN, 1, remoteAddr);
            // 被保护的业务逻辑
            // 查询数据库
            Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
            // 获取封装类
            return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
        } catch (Throwable ex) {
            // 业务异常
            if (!BlockException.isBlockException(ex)) {
                Tracer.trace(ex);
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
            }
            // 降级操作
            if (ex instanceof DegradeException) {
                return handleFallback(questionQueryRequest, request, ex);
            }
            // 限流操作
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "访问过于频繁，请稍后再试");
        } finally {
            if (entry != null) {
                entry.exit(1, remoteAddr);
            }
        }
    }

    /**
     * listQuestionVOByPageSentinel 降级操作：直接返回本地数据（此处为了方便演示，写在同一个类中）
     */
    public BaseResponse<Page<QuestionVO>> handleFallback(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                         HttpServletRequest request, Throwable ex) {
        // 可以返回本地数据或空数据
        return ResultUtils.success(null);
    }

    /**
     * 分页获取当前登录用户创建的题目列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑题目（给用户使用）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 数据校验
        questionService.validQuestion(question, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionEditRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion

    @PostMapping("/search/page/vo")
    public BaseResponse<Page<QuestionVO>> searchQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.searchFromEs(questionQueryRequest);
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    @PostMapping("/delete/batch")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchDeleteQuestions(@RequestBody QuestionBatchDeleteRequest questionBatchDeleteRequest) {
        ThrowUtils.throwIf(questionBatchDeleteRequest == null, ErrorCode.PARAMS_ERROR);
        questionService.batchDeleteQuestions(questionBatchDeleteRequest.getQuestionIdList());
        return ResultUtils.success(true);
    }

    /**
     * AI 生成题目（仅管理员可用）
     *
     * @param questionAIGenerateRequest 请求参数
     * @param request HTTP 请求
     * @return 是否生成成功
     */
    @PostMapping("/ai/generate/question")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> aiGenerateQuestions(@RequestBody QuestionAIGenerateRequest questionAIGenerateRequest, HttpServletRequest request) {
        String questionType = questionAIGenerateRequest.getQuestionType();
        int number = questionAIGenerateRequest.getNumber();
        // 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(questionType), ErrorCode.PARAMS_ERROR, "题目类型不能为空");
        ThrowUtils.throwIf(number <= 0, ErrorCode.PARAMS_ERROR, "题目数量必须大于 0");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用 AI 生成题目服务
        questionService.aiGenerateQuestions(questionType, number, loginUser);
        // 返回结果
        return ResultUtils.success(true);
    }
}
