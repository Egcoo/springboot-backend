package com.quiz.esdao;

import com.quiz.model.dto.post.PostEsDTO;
import com.quiz.model.dto.question.QuestionEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * @author shengjie fan
 * @date 2025/2/19 - 14:06
 */

/**
 * 题目 ES 操作
 *
 * @author shengjie fan
 */
public interface QuestionEsDao extends ElasticsearchRepository<QuestionEsDTO, Long> {

    List<PostEsDTO> findByUserId(Long userId);
}
