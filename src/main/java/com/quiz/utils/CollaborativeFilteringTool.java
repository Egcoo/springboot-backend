package com.quiz.utils;

import java.util.*;

public class CollaborativeFilteringTool {

    // 用户-题目矩阵
    private Map<Long, Set<Long>> userItemMap;

    public CollaborativeFilteringTool() {
        userItemMap = new HashMap<>();
    }

    /**
     * 添加用户记录
     *
     * @param userId 用户ID
     * @param itemId 题目ID
     */
    public void addUserItem(Long userId, Long itemId) {
        userItemMap.computeIfAbsent(userId, k -> new HashSet<>()).add(itemId);
    }

    /**
     * 计算用户之间的余弦相似度
     *
     * @param user1Items 用户1的题目集合
     * @param user2Items 用户2的题目集合
     * @return 余弦相似度
     */
    private double cosineSimilarity(Set<Long> user1Items, Set<Long> user2Items) {
        Set<Long> intersection = new HashSet<>(user1Items);
        intersection.retainAll(user2Items);

        double dotProduct = intersection.size();
        double norm1 = Math.sqrt(user1Items.size());
        double norm2 = Math.sqrt(user2Items.size());

        if (norm1 == 0 || norm2 == 0) {
            return 0;
        }
        return dotProduct / (norm1 * norm2);
    }

    /**
     * 获取与目标用户最相似的用户及其相似度
     *
     * @param targetUserId 目标用户ID
     * @param topN         返回的相似用户数量
     * @return 相似用户及其相似度的映射
     */
    public Map<Long, Double> getSimilarUsersWithSimilarity(Long targetUserId, int topN) {
        Map<Long, Double> similarityMap = new HashMap<>();

        Set<Long> targetUserItems = userItemMap.getOrDefault(targetUserId, new HashSet<>());

        for (Map.Entry<Long, Set<Long>> entry : userItemMap.entrySet()) {
            Long userId = entry.getKey();
            if (userId == targetUserId) continue; // 跳过目标用户自己

            double similarity = cosineSimilarity(targetUserItems, entry.getValue());
            similarityMap.put(userId, similarity);
        }

        // 按相似度排序
        List<Map.Entry<Long, Double>> sortedList = new ArrayList<>(similarityMap.entrySet());
        sortedList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // 返回前topN个相似用户及其相似度
        Map<Long, Double> topSimilarUsers = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(topN, sortedList.size()); i++) {
            topSimilarUsers.put(sortedList.get(i).getKey(), sortedList.get(i).getValue());
        }
        return topSimilarUsers;
    }

    /**
     * 为目标用户生成推荐题目及其推荐分数
     *
     * @param targetUserId 目标用户ID
     * @param topN         返回的推荐题目数量
     * @return 推荐的题目及其推荐分数的映射
     */
    public Map<Long, Double> recommendItemsWithScores(Long targetUserId, int topN) {
        Set<Long> targetUserItems = userItemMap.getOrDefault(targetUserId, new HashSet<>());
        Map<Long, Double> similarUsers = getSimilarUsersWithSimilarity(targetUserId, topN);

        // 统计相似用户的题目，并加权推荐分数
        Map<Long, Double> itemScoreMap = new HashMap<>();
        for (Map.Entry<Long, Double> entry : similarUsers.entrySet()) {
            Long userId = entry.getKey();
            double similarity = entry.getValue();

            for (Long itemId : userItemMap.get(userId)) {
                if (!targetUserItems.contains(itemId)) {
                    itemScoreMap.put(itemId, itemScoreMap.getOrDefault(itemId, 0.0) + similarity);
                }
            }
        }

        // 按题目的推荐分数排序
        List<Map.Entry<Long, Double>> sortedItems = new ArrayList<>(itemScoreMap.entrySet());
        sortedItems.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // 返回前topN个推荐题目及其推荐分数
        Map<Long, Double> recommendedItems = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(topN, sortedItems.size()); i++) {
            recommendedItems.put(sortedItems.get(i).getKey(), sortedItems.get(i).getValue());
        }
        return recommendedItems;
    }

    /**
     * 清空用户-题目数据
     */
    public void clearData() {
        userItemMap.clear();
    }

    /**
     * 获取当前用户-题目数据
     *
     * @return 用户-题目矩阵
     */
    public Map<Long, Set<Long>> getUserItemMap() {
        return userItemMap;
    }


    public static void main(String[] args) {

            // 创建工具类实例
            CollaborativeFilteringTool cfTool = new CollaborativeFilteringTool();

            // 添加用户题目数据
            cfTool.addUserItem(1L, 101L);
            cfTool.addUserItem(1L, 102L);
            cfTool.addUserItem(1L, 103L);

            cfTool.addUserItem(2L, 101L);
            cfTool.addUserItem(2L, 104L);
            cfTool.addUserItem(2L, 105L);

            cfTool.addUserItem(3L, 102L);
            cfTool.addUserItem(3L, 103L);
            cfTool.addUserItem(3L, 106L);

            cfTool.addUserItem(4L, 101L);
            cfTool.addUserItem(4L, 103L);
            cfTool.addUserItem(4L, 107L);

            // 为用户1生成推荐
            Long targetUserId = 1L;
            int topN = 5;
        Map<Long, Set<Long>> userItemMap1 = cfTool.userItemMap;
        System.err.println("用户矩阵：" + userItemMap1);
        // 获取相似用户及其相似度
            Map<Long, Double> similarUsers = cfTool.getSimilarUsersWithSimilarity(targetUserId, topN);
            System.out.println("与用户" + targetUserId + "最相似的用户及其相似度:");
            for (Map.Entry<Long, Double> entry : similarUsers.entrySet()) {
                System.out.println("用户 " + entry.getKey() + " 的相似度: " + entry.getValue());
            }

            // 生成推荐及其推荐分数
            Map<Long, Double> recommendations = cfTool.recommendItemsWithScores(targetUserId, topN);
            Set<Long> integers = recommendations.keySet();
            System.out.println("为用户" + targetUserId + "推荐的题目: " + integers);
            System.out.println("为用户" + targetUserId + "推荐的题目及其推荐分数:");
            for (Map.Entry<Long, Double> entry : recommendations.entrySet()) {
                System.out.println("题目 " + entry.getKey() + " 的推荐分数: " + entry.getValue());
            }

            // 清空数据
            cfTool.clearData();
            System.out.println("数据已清空，当前用户-题目题目数据: " + cfTool.getUserItemMap());
        }

}
