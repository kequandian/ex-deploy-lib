/*
 Navicat Premium Data Transfer

 Source Server         : zzzz
 Source Server Type    : MySQL
 Source Server Version : 50722
 Source Host           : localhost:3306
 Source Schema         : test

 Target Server Type    : MySQL
 Target Server Version : 50722
 File Encoding         : 65001

 Date: 06/08/2020 09:45:05
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_jar
-- ----------------------------
DROP TABLE IF EXISTS `t_jar`;
CREATE TABLE `t_jar`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `app_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '应用标识ID',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'JAR包记录名称',
  `status` varchar(26) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT 'stat' COMMENT '当前记录状态',
  `dependencies` json NOT NULL COMMENT 'JAR包依赖',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '记录更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
