package com.quiz.model.dto.questionBankQuestion;

/**
 * @author Egcoo
 * @date 2025/2/21 - 18:15
 */
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量创建题库题目关联请求
 *
 */
@Data
public class QuestionBankQuestionBatchAddRequest implements Serializable {

    /**
     * 题库 id
     */
    private Long questionBankId;

    /**
     * 题目 id 列表
     * 以前是传递一个题目id，现在是传递一个题目id列表
     */
    private List<Long> questionIdList;

    private static final long serialVersionUID = 1L;
}
