/*
 Navicat MySQL Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50738
 Source Host           : localhost:3306
 Source Schema         : async-task

 Target Server Type    : MySQL
 Target Server Version : 50738
 File Encoding         : 65001

 Date: 16/07/2022 01:50:07
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for async_task_data
-- ----------------------------
DROP TABLE IF EXISTS `async_task_data`;
CREATE TABLE `async_task_data`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `type` int(11) NOT NULL COMMENT '执行器类型id',
  `biz_id` bigint(20) UNSIGNED NOT NULL COMMENT '业务id',
  `biz_data` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务数据',
  `status` int(11) UNSIGNED NOT NULL COMMENT '执行状态 1:待执行；2执行中；3成功；4失败；5已取消',
  `plan_time` bigint(20) UNSIGNED NOT NULL COMMENT '计划执行时间',
  `start_time` bigint(20) UNSIGNED NOT NULL COMMENT '最近一次执行开始时间',
  `end_time` bigint(20) UNSIGNED NOT NULL COMMENT '最近一次执行结束时间',
  `executor` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行器标识',
  `execute_count` int(11) UNSIGNED NOT NULL COMMENT '已执行次数',
  `trace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行时候的traceId，定位问题',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务创建人',
  `create_time` bigint(20) UNSIGNED NOT NULL COMMENT '任务创建时间',
  `update_time` bigint(20) UNSIGNED NOT NULL COMMENT '任务更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of async_task_data
-- ----------------------------
INSERT INTO `async_task_data` VALUES (1, 1, 0, 'wangtianyi', 3, 1, 1657907203717, 1657907206752, 'demo:null:20220716014633805:AsyncTask-39', 2, '', '', 0, 1657907203717);

-- ----------------------------
-- Table structure for async_task_executor
-- ----------------------------
DROP TABLE IF EXISTS `async_task_executor`;
CREATE TABLE `async_task_executor`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `executor` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行器名称',
  `task_id` bigint(20) UNSIGNED NOT NULL COMMENT '任务id',
  `create_time` bigint(20) UNSIGNED NOT NULL COMMENT '创建时间',
  `update_time` bigint(20) UNSIGNED NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1548001126766047234 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of async_task_executor
-- ----------------------------
INSERT INTO `async_task_executor` VALUES (1548000854194982914, 'demo:null:20220716014517239:AsyncTask-38', 1, 1657907138661, 1657907138661);

-- ----------------------------
-- Table structure for async_task_executor_config
-- ----------------------------
DROP TABLE IF EXISTS `async_task_executor_config`;
CREATE TABLE `async_task_executor_config`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行器类型，实现类全称',
  `parallel_current` int(11) UNSIGNED NOT NULL COMMENT '当前运行任务个数',
  `parallel_max` int(11) UNSIGNED NOT NULL COMMENT '最大并行度',
  `status` int(11) UNSIGNED NOT NULL COMMENT '是否生效',
  `next_time` bigint(20) UNSIGNED NOT NULL COMMENT '下次调度时间',
  `owners` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '负责人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of async_task_executor_config
-- ----------------------------
INSERT INTO `async_task_executor_config` VALUES (1, 'com.wty.async.task.service.AsyncTaskDispatchService', 10, 10, 0, 0, 'wangtianyi');
INSERT INTO `async_task_executor_config` VALUES (2, 'asyncTaskExecutorDemo', 2, 3, 1, 1657733695048, 'wangtianyi');

SET FOREIGN_KEY_CHECKS = 1;
