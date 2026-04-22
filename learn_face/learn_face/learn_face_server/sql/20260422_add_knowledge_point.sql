ALTER TABLE `question`
ADD COLUMN IF NOT EXISTS `knowledge_point` varchar(255) DEFAULT NULL COMMENT '知识点标签';

ALTER TABLE `task`
ADD COLUMN IF NOT EXISTS `knowledge_point` varchar(255) DEFAULT NULL COMMENT '知识点标签';
