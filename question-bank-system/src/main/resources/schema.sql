-- 题库系统数据库设计
-- 创建时间: 2026-03-07
-- 作者: 题库系统开发团队
create database tiku;

use tiku;

-- ==============
-- 1. 用户表 (users)
-- ==============
-- 存储系统用户信息，包括普通用户、管理员等
CREATE TABLE users (
                       id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID，主键',
                       username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名，唯一',
                       email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱，唯一',
                       password VARCHAR(255) NOT NULL COMMENT '密码，加密存储',
                       avatar VARCHAR(255) DEFAULT '' COMMENT '头像URL',
                       bio TEXT COMMENT '个人简介',
                       role ENUM('user', 'admin') NOT NULL DEFAULT 'user' COMMENT '用户角色：user-普通用户, admin-管理员',
                       status ENUM('active', 'disabled', 'pending') NOT NULL DEFAULT 'active' COMMENT '用户状态：active-激活, disabled-禁用, pending-待审核',
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ==============
-- 2. 分类表 (categories)
-- ==============
-- 存储题目分类信息，支持多级分类
CREATE TABLE categories (
                            id INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID，主键',
                            name VARCHAR(100) NOT NULL COMMENT '分类名称',
                            parent_id INT DEFAULT NULL COMMENT '父分类ID，NULL表示顶级分类',
                            sort INT DEFAULT 0 COMMENT '排序权重，数值越小越靠前',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- ==============
-- 3. 题库表 (banks)
-- ==============
-- 存储题库信息，每个题库包含多个题目
CREATE TABLE banks (
                       id INT PRIMARY KEY AUTO_INCREMENT COMMENT '题库ID，主键',
                       name VARCHAR(200) NOT NULL COMMENT '题库名称',
                       description TEXT COMMENT '题库描述',
                       category_id INT NOT NULL COMMENT '所属分类ID',
                       author_id INT NOT NULL COMMENT '作者ID（用户ID）',
                       visibility ENUM('public', 'private') NOT NULL DEFAULT 'public' COMMENT '可见性：public-公开, private-私有',
                       status ENUM('draft', 'pending', 'published', 'rejected') NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿, pending-待审核, published-已发布, rejected-已拒绝',
                       question_count INT DEFAULT 0 COMMENT '题目数量',
                       favorite_count INT DEFAULT 0 COMMENT '收藏数',
                       view_count INT DEFAULT 0 COMMENT '浏览数',
                       like_count INT DEFAULT 0 COMMENT '点赞数',
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
                       FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
                       FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- ==============
-- 4. 题目表 (questions)
-- ==============
-- 存储具体题目信息
CREATE TABLE questions (
                           id INT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID，主键',
                           bank_id INT NOT NULL COMMENT '所属题库ID',
                           type ENUM('single', 'multiple', 'judge', 'fill', 'essay') NOT NULL COMMENT '题目类型：single-单选, multiple-多选, judge-判断, fill-填空, essay-问答',
                           content TEXT NOT NULL COMMENT '题目内容',
                           options JSON DEFAULT NULL COMMENT '选项数据（JSON格式），单选/多选题目使用',
                           answer JSON NOT NULL COMMENT '答案数据（JSON格式），不同题型格式不同',
                           analysis TEXT COMMENT '答案解析',
                           difficulty ENUM('easy', 'medium', 'hard') NOT NULL DEFAULT 'medium' COMMENT '难度等级：easy-简单, medium-中等, hard-困难',
                           author_id INT NOT NULL COMMENT '作者ID（用户ID）',
                           status ENUM('draft', 'pending', 'published', 'rejected') NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿, pending-待审核, published-已发布, rejected-已拒绝',
                           view_count INT DEFAULT 0 COMMENT '浏览数',
                           like_count INT DEFAULT 0 COMMENT '点赞数',
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           FOREIGN KEY (bank_id) REFERENCES banks(id) ON DELETE CASCADE,
                           FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- ==============
-- 5. 收藏表 (favorites)
-- ==============
-- 存储用户收藏的题库和题目
CREATE TABLE favorites (
                           id INT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID，主键',
                           user_id INT NOT NULL COMMENT '用户ID',
                           target_type ENUM('bank', 'question') NOT NULL COMMENT '收藏目标类型：bank-题库, question-题目',
                           target_id INT NOT NULL COMMENT '收藏目标ID',
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                           UNIQUE KEY uk_user_target (user_id, target_type, target_id) COMMENT '确保用户不能重复收藏同一目标',
                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ==============
-- 6. 点赞表 (likes)
-- ==============
-- 存储用户点赞的题库和题目
CREATE TABLE likes (
                       id INT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID，主键',
                       user_id INT NOT NULL COMMENT '用户ID',
                       target_type ENUM('bank', 'question') NOT NULL COMMENT '点赞目标类型：bank-题库, question-题目',
                       target_id INT NOT NULL COMMENT '点赞目标ID',
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                       UNIQUE KEY uk_user_target (user_id, target_type, target_id) COMMENT '确保用户不能重复点赞同一目标',
                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- ==============
-- 7. 关注表 (follows)
-- ==============
-- 存储用户关注关系
CREATE TABLE follows (
                         id INT PRIMARY KEY AUTO_INCREMENT COMMENT '关注ID，主键',
                         follower_id INT NOT NULL COMMENT '关注者ID',
                         following_id INT NOT NULL COMMENT '被关注者ID',
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
                         UNIQUE KEY uk_follower_following (follower_id, following_id) COMMENT '确保用户不能重复关注同一用户',
                         FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
                         FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注表';

-- ==============
-- 8. 评论表 (comments)
-- ==============
-- 存储题目评论
CREATE TABLE comments (
                          id INT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID，主键',
                          question_id INT NOT NULL COMMENT '题目ID',
                          user_id INT NOT NULL COMMENT '用户ID',
                          content TEXT NOT NULL COMMENT '评论内容',
                          parent_id INT DEFAULT NULL COMMENT '父评论ID，NULL表示顶级评论',
                          status ENUM('active', 'deleted') NOT NULL DEFAULT 'active' COMMENT '评论状态：active-正常, deleted-已删除',
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE ,
                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ,
                          FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ==============
-- 9. 用户会话表 (sessions)
-- ==============
-- 存储用户登录会话信息
CREATE TABLE sessions (
                          id VARCHAR(255) PRIMARY KEY COMMENT '会话ID（token）',
                          user_id INT NOT NULL COMMENT '用户ID',
                          ip_address VARCHAR(45) COMMENT 'IP地址',
                          user_agent TEXT COMMENT '用户代理',
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';

-- ==============
-- 索引优化
-- ==============

-- 用户表索引
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- 分类表索引
CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_sort ON categories(sort);

-- 题库表索引
CREATE INDEX idx_banks_category ON banks(category_id);
CREATE INDEX idx_banks_author ON banks(author_id);
CREATE INDEX idx_banks_status ON banks(status);
CREATE INDEX idx_banks_visibility ON banks(visibility);
CREATE INDEX idx_banks_created ON banks(created_at);

-- 题目表索引
CREATE INDEX idx_questions_bank ON questions(bank_id);
CREATE INDEX idx_questions_author ON questions(author_id);
CREATE INDEX idx_questions_type ON questions(type);
CREATE INDEX idx_questions_difficulty ON questions(difficulty);
CREATE INDEX idx_questions_status ON questions(status);
CREATE INDEX idx_questions_created ON questions(created_at);

-- 收藏表索引
CREATE INDEX idx_favorites_user ON favorites(user_id);
CREATE INDEX idx_favorites_target ON favorites(target_type, target_id);

-- 点赞表索引
CREATE INDEX idx_likes_user ON likes(user_id);
CREATE INDEX idx_likes_target ON likes(target_type, target_id);

-- 关注表索引
CREATE INDEX idx_follows_follower ON follows(follower_id);
CREATE INDEX idx_follows_following ON follows(following_id);

-- 评论表索引
CREATE INDEX idx_comments_question ON comments(question_id);
CREATE INDEX idx_comments_user ON comments(user_id);
CREATE INDEX idx_comments_parent ON comments(parent_id);
CREATE INDEX idx_comments_status ON comments(status);

-- ==============
-- 初始数据
-- ==============

-- 插入管理员用户
INSERT INTO users (username, email, password, role, status) VALUES
    ('admin', 'admin@example.com', '$2a$10$example_hashed_password', 'admin', 'active');

-- 插入顶级分类
INSERT INTO categories (name, parent_id, sort) VALUES
                                                   ('编程语言', NULL, 1),
                                                   ('前端开发', NULL, 2),
                                                   ('后端开发', NULL, 3),
                                                   ('数据库', NULL, 4),
                                                   ('算法与数据结构', NULL, 5);

-- 插入子分类
INSERT INTO categories (name, parent_id, sort) VALUES
                                                   ('JavaScript', 1, 1),
                                                   ('Python', 1, 2),
                                                   ('Java', 1, 3),
                                                   ('C/C++', 1, 4),
                                                   ('Go', 1, 5),
                                                   ('Vue.js', 2, 1),
                                                   ('React', 2, 2),
                                                   ('Angular', 2, 3),
                                                   ('CSS/HTML', 2, 4),
                                                   ('TypeScript', 2, 5),
                                                   ('Node.js', 3, 1),
                                                   ('Spring', 3, 2),
                                                   ('Django', 3, 3),
                                                   ('Express/Koa', 3, 4),
                                                   ('NestJS', 3, 5),
                                                   ('MySQL', 4, 1),
                                                   ('MongoDB', 4, 2),
                                                   ('Redis', 4, 3),
                                                   ('PostgreSQL', 4, 4),
                                                   ('Oracle', 4, 5),
                                                   ('基础算法', 5, 1),
                                                   ('数据结构', 5, 2),
                                                   ('LeetCode', 5, 3),
                                                   ('设计模式', 5, 4),
                                                   ('系统设计', 5, 5);

-- ==============
-- 常用查询视图
-- ==============

-- 题库详情视图（包含分类和作者信息）
CREATE VIEW bank_details AS
SELECT
    b.*,
    c.name as category_name,
    u.username as author_name
FROM banks b
         JOIN categories c ON b.category_id = c.id
         JOIN users u ON b.author_id = u.id;

-- 题目详情视图（包含题库、分类和作者信息）
CREATE VIEW question_details AS
SELECT
    q.*,
    b.name as bank_name,
    c.name as category_name,
    u.username as author_name
FROM questions q
         JOIN banks b ON q.bank_id = b.id
         JOIN categories c ON b.category_id = c.id
         JOIN users u ON q.author_id = u.id;

-- ==============
-- 数据库设计说明
-- ==============

/*
表关系说明：
1. users 表是核心用户表，其他表通过 author_id 或 user_id 引用
2. categories 表支持无限级分类，通过 parent_id 实现自引用
3. banks 表属于某个分类，由某个用户创建
4. questions 表属于某个题库，由某个用户创建
5. favorites/likes 表实现多态收藏/点赞功能，通过 target_type 和 target_id 区分目标类型
6. follows 表实现用户关注关系
7. comments 表实现题目评论功能，支持嵌套评论

安全考虑：
1. 所有外键都设置了适当的级联删除策略
2. 用户密码使用 bcrypt 加密存储
3. 会话表包含 IP 地址和 User-Agent 用于安全审计
4. 状态字段控制数据可见性和审核流程

性能优化：
1. 为常用查询字段创建了复合索引
2. 使用 JSON 类型存储结构化数据（如题目选项和答案）
3. 预计算计数字段（如 question_count, favorite_count 等）避免实时聚合
4. 创建常用查询视图简化复杂查询

扩展性：
1. 多态设计支持未来扩展更多可收藏/点赞的对象类型
2. 题目类型枚举支持未来添加新题型
3. 权限系统通过 role 字段支持多角色扩展
*/

-- ============================================================
-- 练习/考试模式 & 错题本扩展表
-- ============================================================

-- 10. 答题会话表 (practice_sessions)
CREATE TABLE IF NOT EXISTS practice_sessions (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id      INT          NOT NULL COMMENT '用户ID',
    bank_id      INT          DEFAULT NULL COMMENT '题库ID，NULL表示跨库练习',
    question_ids TEXT         NOT NULL COMMENT '题目ID列表，JSON数组',
    user_answers TEXT         DEFAULT NULL COMMENT '用户答案，JSON对象 {questionId: answer}',
    score        INT          NOT NULL DEFAULT 0 COMMENT '得分',
    total_score  INT          NOT NULL DEFAULT 0 COMMENT '满分',
    time_limit   INT          NOT NULL DEFAULT 0 COMMENT '时间限制（秒），0=不限时',
    time_used    INT          NOT NULL DEFAULT 0 COMMENT '实际用时（秒）',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=进行中 1=已提交',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    submitted_at DATETIME     DEFAULT NULL COMMENT '提交时间',
    INDEX idx_ps_user_id (user_id),
    INDEX idx_ps_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题会话表';

-- 11. 错题本表 (wrong_questions)
CREATE TABLE IF NOT EXISTS wrong_questions (
    id              BIGINT      AUTO_INCREMENT PRIMARY KEY,
    user_id         INT         NOT NULL COMMENT '用户ID',
    question_id     INT         NOT NULL COMMENT '题目ID',
    wrong_count     INT         NOT NULL DEFAULT 1 COMMENT '累计答错次数',
    last_wrong_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近答错时间',
    mastered        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否已掌握 0=未掌握 1=已掌握',
    starred         TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否收藏 0=未收藏 1=已收藏',
    error_reason    VARCHAR(50) DEFAULT NULL COMMENT '错因：careless/unknown/concept',
    next_review_at  DATETIME    DEFAULT NULL COMMENT '下次复习时间（艾宾浩斯）',
    review_count    INT         NOT NULL DEFAULT 0 COMMENT '已复习次数',
    UNIQUE KEY uk_user_question (user_id, question_id),
    INDEX idx_wq_user_id (user_id),
    INDEX idx_wq_last_wrong (user_id, last_wrong_at),
    INDEX idx_wq_next_review (user_id, next_review_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- 12. 复习打卡记录表 (review_records)
CREATE TABLE IF NOT EXISTS review_records (
    id           BIGINT     AUTO_INCREMENT PRIMARY KEY,
    user_id      INT        NOT NULL COMMENT '用户ID',
    question_id  INT        NOT NULL COMMENT '题目ID',
    reviewed_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '复习时间',
    result       TINYINT(1) DEFAULT NULL COMMENT '复习结果：1=答对 0=答错',
    INDEX idx_rr_user_date (user_id, reviewed_at),
    INDEX idx_rr_user_question (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复习打卡记录表';


ALTER TABLE questions
    drop COLUMN visibility ;
