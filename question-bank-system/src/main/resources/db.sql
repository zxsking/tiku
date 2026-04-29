-- ============================================================
-- 题库系统测试数据 - 100条关联数据
-- 创建时间: 2026-03-08
-- ============================================================

USE tiku;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE comments;
TRUNCATE TABLE sessions;
TRUNCATE TABLE follows;
TRUNCATE TABLE likes;
TRUNCATE TABLE favorites;
TRUNCATE TABLE questions;
TRUNCATE TABLE banks;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;
-- ==============
-- 1. 用户数据 (10个用户)
-- ==============
INSERT INTO users (username, email, password, avatar, bio, role, status) VALUES
                                                                             ('admin','admin@gmail.com','$2a$10$KZWsX0qMq8eY.Jvl/0RVt.V.aEUm/T3Dl2T3Tt67zS2ruDvAk9qWe','https://api.dicebear.com/7.x/avataaars/svg?seed=zhang','Java高级工程师，专注后端开发5年', 'admin', 'active'),
                                                                             ('zhang_wei',    'zhang@example.com',   '$2a$10$xKp1Qz3mN8vL2oR5tY7uIeW4hJ6kD0bG9sF1cA3nM8pE5rT2wV',  'https://api.dicebear.com/7.x/avataaars/svg?seed=zhang',   'Java高级工程师，专注后端开发5年', 'user', 'active'),
                                                                             ('li_fang',      'li@example.com',      '$2a$10$aB2cD3eF4gH5iJ6kL7mN8oP9qR0sT1uV2wX3yZ4aB5cD6eF7gH8i', 'https://api.dicebear.com/7.x/avataaars/svg?seed=li',      '前端开发者，Vue/React爱好者', 'user', 'active'),
                                                                             ('wang_jun',     'wang@example.com',    '$2a$10$bC3dE4fG5hI6jK7lM8nO9pQ0rS1tU2vW3xY4zA5bC6dE7fG8hI9j', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wang',    '算法竞赛选手，LeetCode 2000+', 'user', 'active'),
                                                                             ('chen_xia',     'chen@example.com',    '$2a$10$cD4eF5gH6iJ7kL8mN9oP0qR1sT2uV3wX4yZ5aC6bD7eF8gH9iJ0k', 'https://api.dicebear.com/7.x/avataaars/svg?seed=chen',    '全栈开发，Python/Django专家', 'user', 'active'),
                                                                             ('zhao_yang',    'zhao@example.com',    '$2a$10$dE5fG6hI7jK8lM9nO0pQ1rS2tU3vW4xY5zB6aC7bD8eF9gH0iJ1l', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhao',    '数据库专家，MySQL/Redis架构师', 'user', 'active'),
                                                                             ('liu_ming',     'liu@example.com',     '$2a$10$eF6gH7iJ8kL9mN0oP1qR2sT3uV4wX5yZ6aC7bD8cE9fG0hI1jK2m', 'https://api.dicebear.com/7.x/avataaars/svg?seed=liu',     'Go语言布道者，微服务架构', 'user', 'active'),
                                                                             ('sun_ling',     'sun@example.com',     '$2a$10$fG7hI8jK9lM0nO1pQ2rS3tU4vW5xY6zB7aC8bD9cE0fG1hI2jK3n', 'https://api.dicebear.com/7.x/avataaars/svg?seed=sun',     '前端工程师，TypeScript重度用户', 'user', 'active'),
                                                                             ('zhou_fei',     'zhou@example.com',    '$2a$10$gH8iJ9kL0mN1oP2qR3sT4uV5wX6yZ7aB8cD9eF0gH1iJ2kL3mN4o', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhou',    'Spring生态专家，微服务开发', 'user', 'active'),
                                                                             ('wu_tao',       'wu@example.com',      '$2a$10$hI9jK0lM1nO2pQ3rS4tU5vW6xY7zB8aC9bD0eF1gH2iJ3kL4mN5p', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wu',      '安全工程师，CTF爱好者', 'user', 'active'),
                                                                             ('zheng_lan',    'zheng@example.com',   '$2a$10$iJ0kL1mN2oP3qR4sT5uV6wX7yZ8aB9cD0eF1gH2iJ3kL4mN5oP6q', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zheng',   '机器学习工程师，Python数据分析', 'user', 'active');

-- ==============
-- 2. 题库数据 (10个题库，覆盖不同分类)
-- ==============
-- 注：category_id 对应之前插入的子分类，author_id 对应新插入用户(id从2开始)
INSERT INTO banks (name, description, category_id, author_id, visibility, status, question_count, favorite_count, view_count, like_count) VALUES
                                                                                                                                              ('Java核心知识点精讲',      'Java基础到进阶，涵盖OOP、集合、多线程、JVM等核心知识点，适合备战面试', 8,  2,  'public',  'published', 10, 35, 1280, 88),
                                                                                                                                              ('Vue3全栈开发题库',        'Vue3 Composition API、Pinia、Vue Router全面考察，适合前端进阶', 11, 3,  'public',  'published', 10, 42, 960,  75),
                                                                                                                                              ('LeetCode经典100题',       '精选LeetCode高频算法题，包含详细题解与复杂度分析', 23, 4,  'public',  'published', 10, 88, 3400, 210),
                                                                                                                                              ('MySQL性能优化实战',       '从索引原理到执行计划，深入讲解MySQL调优技巧', 16, 5,  'public',  'published', 10, 56, 2100, 130),
                                                                                                                                              ('Go语言并发编程',          'Goroutine、Channel、sync包深度解析，并发模式最佳实践', 10, 6,  'public',  'published', 10, 29, 780,  65),
                                                                                                                                              ('TypeScript高级特性',      '泛型、装饰器、类型体操、工程化配置全面覆盖', 15, 7,  'public',  'published', 10, 38, 1150, 92),
                                                                                                                                              ('Spring Boot面试宝典',     'Spring IOC/AOP原理、SpringBoot自动装配、微服务核心考点', 17, 8,  'public',  'published', 10, 71, 2600, 185),
                                                                                                                                              ('Python数据分析基础',      'NumPy、Pandas、Matplotlib核心API与实战应用', 7,  9,  'public',  'published', 10, 22, 640,  48),
                                                                                                                                              ('Redis缓存设计模式',       '数据结构选择、缓存穿透/击穿/雪崩解决方案、分布式锁', 18, 10, 'public',  'published', 10, 47, 1730, 115),
                                                                                                                                              ('前端设计模式精讲',        '观察者、发布订阅、工厂、单例等设计模式在前端的实际应用', 24, 11, 'public',  'published', 10, 31, 890,  72);

-- ==============
-- 3. 题目数据 (每个题库10题，共100题)
-- ==============

-- ---- 题库1: Java核心知识点精讲 (bank_id=1, author_id=2) ----
INSERT INTO questions (bank_id, type, content, options, answer, analysis, difficulty, author_id, status, view_count, like_count) VALUES
                                                                                                                                     (1, 'single', 'Java中，以下哪个关键字用于防止方法被子类重写？',
                                                                                                                                      '[{"key":"A","value":"static"},{"key":"B","value":"private"},{"key":"C","value":"final"},{"key":"D","value":"abstract"}]',
                                                                                                                                      '"C"',
                                                                                                                                      '`final`修饰的方法不能被子类重写（override）。`static`方法属于类，不参与多态；`private`方法对子类不可见；`abstract`方法必须被子类实现。',
                                                                                                                                      'easy', 2, 'published', 320, 18),

                                                                                                                                     (1, 'multiple', '以下关于Java集合框架说法正确的是？',
                                                                                                                                      '[{"key":"A","value":"ArrayList是线程安全的"},{"key":"B","value":"HashMap允许null键和null值"},{"key":"C","value":"LinkedList实现了Deque接口"},{"key":"D","value":"HashSet底层基于HashMap实现"}]',
                                                                                                                                      '["B","C","D"]',
                                                                                                                                      'A错误，ArrayList非线程安全，Vector才是；B正确，HashMap允许一个null键和多个null值；C正确，LinkedList同时实现List和Deque；D正确，HashSet内部持有一个HashMap。',
                                                                                                                                      'medium', 2, 'published', 415, 24),

                                                                                                                                     (1, 'judge', 'Java中int和Integer可以直接用==比较是否相等。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      '基本类型int用==比较值，但Integer是对象，==比较引用地址。Integer有缓存池（-128~127），该范围内==相等，范围外则不等。应使用equals()比较Integer。',
                                                                                                                                      'easy', 2, 'published', 280, 15),

                                                                                                                                     (1, 'single', 'Java内存模型中，以下哪个区域是线程共享的？',
                                                                                                                                      '[{"key":"A","value":"程序计数器"},{"key":"B","value":"虚拟机栈"},{"key":"C","value":"堆"},{"key":"D","value":"本地方法栈"}]',
                                                                                                                                      '"C"',
                                                                                                                                      'JVM运行时数据区分为线程私有（程序计数器、虚拟机栈、本地方法栈）和线程共享（堆、方法区/元空间）两类。堆是最大的内存区域，存储对象实例。',
                                                                                                                                      'medium', 2, 'published', 350, 21),

                                                                                                                                     (1, 'essay', '请简述Java中synchronized和ReentrantLock的区别。',
                                                                                                                                      NULL,
                                                                                                                                      '"synchronized是JVM内置关键字，自动释放锁，不可中断等待；ReentrantLock是显式锁，需手动释放，支持tryLock超时、lockInterruptibly可中断、公平锁、多条件变量等高级功能。"',
                                                                                                                                      '考察Java并发锁机制的理解深度，重点区分两者在使用方式、功能特性和适用场景上的差异。',
                                                                                                                                      'hard', 2, 'published', 490, 32),

                                                                                                                                     (1, 'fill', 'Java中，实现深拷贝的方式有：序列化反序列化、____、实现Cloneable接口重写clone方法。',
                                                                                                                                      NULL,
                                                                                                                                      '"手动递归复制（构造函数逐层复制）"',
                                                                                                                                      '深拷贝的三种主要方式：1.实现Cloneable并重写clone；2.序列化/反序列化；3.手动递归复制每个引用字段。',
                                                                                                                                      'medium', 2, 'published', 195, 11),

                                                                                                                                     (1, 'single', '以下关于Java泛型类型擦除，说法错误的是？',
                                                                                                                                      '[{"key":"A","value":"泛型信息在编译后被擦除"},{"key":"B","value":"运行时可以通过反射获取泛型参数"},{"key":"C","value":"List<String>和List<Integer>在运行时是相同类型"},{"key":"D","value":"类型擦除导致无法创建泛型数组"}]',
                                                                                                                                      '"B"',
                                                                                                                                      '类型擦除后，运行时无法直接获取泛型参数（对于局部变量）。但对于类/接口/方法签名上的泛型，可通过反射的getGenericSuperclass等方法获取，所以B的说法有一定道理，但通常意义上B被认为是错误的描述，因为擦除后运行时泛型信息不保留在实例层面。',
                                                                                                                                      'hard', 2, 'published', 310, 19),

                                                                                                                                     (1, 'multiple', '以下哪些是Java 8引入的新特性？',
                                                                                                                                      '[{"key":"A","value":"Lambda表达式"},{"key":"B","value":"Stream API"},{"key":"C","value":"var关键字"},{"key":"D","value":"Optional类"}]',
                                                                                                                                      '["A","B","D"]',
                                                                                                                                      'Java 8引入了Lambda、Stream API、Optional、默认方法、新日期API等；var关键字是Java 10引入的局部变量类型推断特性。',
                                                                                                                                      'easy', 2, 'published', 420, 26),

                                                                                                                                     (1, 'judge', 'volatile关键字能保证操作的原子性。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      'volatile只保证可见性和有序性（禁止指令重排），不保证原子性。例如i++操作（读-改-写）即使用volatile修饰也不是原子操作，需要用AtomicInteger或synchronized。',
                                                                                                                                      'medium', 2, 'published', 380, 22),

                                                                                                                                     (1, 'essay', '解释Java中的双亲委派模型及其作用。',
                                                                                                                                      NULL,
                                                                                                                                      '"双亲委派模型：类加载时，先委托父类加载器尝试加载，父加载器无法加载才由自身加载。层次：BootstrapCL→ExtCL→AppCL。作用：避免类重复加载，保护核心类库安全（防止自定义java.lang.String覆盖）。"',
                                                                                                                                      '考察JVM类加载机制，理解类加载器层次结构和委派机制的设计初衷。',
                                                                                                                                      'hard', 2, 'published', 445, 28),

-- ---- 题库2: Vue3全栈开发题库 (bank_id=2, author_id=3) ----
                                                                                                                                     (2, 'single', 'Vue3中，以下哪个API用于创建响应式的基本类型数据？',
                                                                                                                                      '[{"key":"A","value":"reactive()"},{"key":"B","value":"ref()"},{"key":"C","value":"computed()"},{"key":"D","value":"watch()"}]',
                                                                                                                                      '"B"',
                                                                                                                                      'ref()用于创建响应式的基本类型（也可包裹对象），通过.value访问；reactive()用于创建响应式对象，不能包裹基本类型；computed()创建计算属性；watch()用于侦听。',
                                                                                                                                      'easy', 3, 'published', 280, 16),

                                                                                                                                     (2, 'multiple', '以下关于Vue3 Composition API的说法，正确的是？',
                                                                                                                                      '[{"key":"A","value":"setup()在beforeCreate之前执行"},{"key":"B","value":"setup()中可以访问this"},{"key":"C","value":"ref创建的响应式数据在模板中自动解包"},{"key":"D","value":"reactive()创建的对象可以被解构保持响应性"}]',
                                                                                                                                      '["A","C"]',
                                                                                                                                      'A正确，setup是最早的生命周期钩子；B错误，setup中没有this；C正确，模板中ref自动解包不需要.value；D错误，reactive对象解构后会失去响应性，需用toRefs()。',
                                                                                                                                      'medium', 3, 'published', 340, 21),

                                                                                                                                     (2, 'judge', 'Vue3中watchEffect会自动收集依赖，无需显式声明监听目标。',
                                                                                                                                      NULL,
                                                                                                                                      '"true"',
                                                                                                                                      'watchEffect会立即执行传入的函数，并自动追踪其中访问的响应式依赖，当依赖变化时重新执行。相比watch需要显式指定依赖源，watchEffect更简洁。',
                                                                                                                                      'easy', 3, 'published', 210, 13),

                                                                                                                                     (2, 'single', 'Vue3中，以下哪种方式可以在子组件中修改父组件传入的props？',
                                                                                                                                      '[{"key":"A","value":"直接修改props对象的属性"},{"key":"B","value":"使用v-model双向绑定"},{"key":"C","value":"通过emit触发父组件事件来修改"},{"key":"D","value":"使用reactive包裹props后修改"}]',
                                                                                                                                      '"C"',
                                                                                                                                      'props是单向数据流，子组件不应直接修改。正确做法是子组件emit事件，父组件监听并更新数据。v-model本质也是props+emit的语法糖，但选项C更准确描述了底层机制。',
                                                                                                                                      'medium', 3, 'published', 295, 18),

                                                                                                                                     (2, 'essay', '请描述Vue3中provide/inject的使用场景及注意事项。',
                                                                                                                                      NULL,
                                                                                                                                      '"provide/inject用于祖先组件向深层子孙组件传递数据，避免props层层透传（prop drilling）。使用时：provide在父组件提供数据，inject在子孙组件注入。注意：默认inject的数据不是响应式的，如需响应式需provide ref/reactive对象；推荐使用readonly包裹避免子组件意外修改。"',
                                                                                                                                      '考察Vue3依赖注入机制的理解和实际应用能力。',
                                                                                                                                      'medium', 3, 'published', 320, 20),

                                                                                                                                     (2, 'fill', 'Vue3中，使用____函数可以将reactive对象转换为具有响应性的ref集合，解决解构丢失响应性的问题。',
                                                                                                                                      NULL,
                                                                                                                                      '"toRefs()"',
                                                                                                                                      'toRefs()将reactive对象的每个属性转换为对应的ref，解构后依然保持响应性。toRef()则针对单个属性。',
                                                                                                                                      'easy', 3, 'published', 175, 10),

                                                                                                                                     (2, 'multiple', '以下哪些是Vue3相对于Vue2的主要改进？',
                                                                                                                                      '[{"key":"A","value":"Composition API"},{"key":"B","value":"Proxy替代Object.defineProperty"},{"key":"C","value":"更好的TypeScript支持"},{"key":"D","value":"虚拟DOM完全重写，性能大幅提升"}]',
                                                                                                                                      '["A","B","C","D"]',
                                                                                                                                      '以上全部正确：A-Composition API解决逻辑复用问题；B-Proxy响应系统能检测数组变化、新增属性；C-源码用TS重写，类型推断更好；D-编译器优化（静态提升、Patch Flags）大幅提升渲染性能。',
                                                                                                                                      'easy', 3, 'published', 390, 25),

                                                                                                                                     (2, 'judge', 'Vue3的Teleport组件可以将组件渲染到DOM中的任意位置。',
                                                                                                                                      NULL,
                                                                                                                                      '"true"',
                                                                                                                                      'Teleport允许将组件内容渲染到DOM树中指定的目标节点（通过to属性指定），常用于Modal、Toast等需要脱离父元素层叠上下文的场景。',
                                                                                                                                      'easy', 3, 'published', 185, 12),

                                                                                                                                     (2, 'single', 'Pinia相比Vuex的优势，以下说法错误的是？',
                                                                                                                                      '[{"key":"A","value":"Pinia不需要mutations，直接在actions中修改state"},{"key":"B","value":"Pinia支持多个store，每个store独立"},{"key":"C","value":"Pinia必须通过mapState等辅助函数才能在组件中使用"},{"key":"D","value":"Pinia有更好的TypeScript类型推断"}]',
                                                                                                                                      '"C"',
                                                                                                                                      'C错误，Pinia在Composition API中可直接使用useXxxStore()，无需辅助函数，简单直接。其他选项均是Pinia相对Vuex的优势。',
                                                                                                                                      'medium', 3, 'published', 270, 17),

                                                                                                                                     (2, 'essay', '解释Vue3中的异步组件和Suspense的使用方式。',
                                                                                                                                      NULL,
                                                                                                                                      '"异步组件：defineAsyncComponent(() => import(./Comp.vue))实现代码分割，配合loadingComponent和errorComponent提供加载状态。Suspense：专门处理异步依赖的内置组件，#default插槽放异步内容，#fallback插槽放加载占位，当所有异步依赖resolve后显示default内容。"',
                                                                                                                                      '考察Vue3代码分割和异步渲染的实践能力。',
                                                                                                                                      'hard', 3, 'published', 310, 20),

-- ---- 题库3: LeetCode经典100题 (bank_id=3, author_id=4) ----
                                                                                                                                     (3, 'single', '两数之和问题：给定数组[2,7,11,15]和目标值9，最优解的时间复杂度是？',
                                                                                                                                      '[{"key":"A","value":"O(n²)"},{"key":"B","value":"O(n log n)"},{"key":"C","value":"O(n)"},{"key":"D","value":"O(1)"}]',
                                                                                                                                      '"C"',
                                                                                                                                      '使用哈希表一次遍历：遍历数组时，检查target-nums[i]是否在哈希表中，若存在则返回结果，否则将nums[i]存入哈希表。时间O(n)，空间O(n)。',
                                                                                                                                      'easy', 4, 'published', 580, 38),

                                                                                                                                     (3, 'essay', '请描述快速排序的算法思路，并分析其时间复杂度。',
                                                                                                                                      NULL,
                                                                                                                                      '"快速排序：选取pivot，partition将数组分为小于pivot和大于pivot两部分，递归排序两部分。平均时间O(n log n)，最坏O(n²)（已排序数组+固定pivot），空间O(log n)递归栈。优化：随机选pivot、三路划分处理重复元素。"',
                                                                                                                                      '经典排序算法，考察分治思想和复杂度分析。',
                                                                                                                                      'medium', 4, 'published', 490, 31),

                                                                                                                                     (3, 'single', '以下哪种数据结构最适合实现LRU缓存（O(1)时间的get和put）？',
                                                                                                                                      '[{"key":"A","value":"数组"},{"key":"B","value":"堆"},{"key":"C","value":"哈希表+双向链表"},{"key":"D","value":"平衡二叉树"}]',
                                                                                                                                      '"C"',
                                                                                                                                      'LRU需要O(1)查找和O(1)移动节点到头部/删除尾部。哈希表提供O(1)查找，双向链表提供O(1)的节点移动（已知节点指针的情况下）。Java中LinkedHashMap即此结构。',
                                                                                                                                      'medium', 4, 'published', 520, 33),

                                                                                                                                     (3, 'judge', '二分查找要求输入数组必须是有序的。',
                                                                                                                                      NULL,
                                                                                                                                      '"true"',
                                                                                                                                      '二分查找的核心前提是数组有序，每次将搜索范围缩小一半，时间复杂度O(log n)。无序数组需先排序O(n log n)，通常不如直接线性查找。',
                                                                                                                                      'easy', 4, 'published', 310, 19),

                                                                                                                                     (3, 'essay', '解释动态规划的核心思想，并用爬楼梯问题（每次可爬1或2阶，n阶楼梯有多少种方法）举例说明。',
                                                                                                                                      NULL,
                                                                                                                                      '"DP核心：将问题分解为重叠子问题，保存子问题结果避免重复计算（记忆化/自底向上）。爬楼梯：dp[i]=dp[i-1]+dp[i-2]，到第i阶=从i-1阶爬1步+从i-2阶爬2步。初始条件dp[1]=1,dp[2]=2。本质是斐波那契数列，可优化为O(1)空间。"',
                                                                                                                                      '动态规划入门经典题，考察DP思想和状态转移方程建立能力。',
                                                                                                                                      'medium', 4, 'published', 620, 40),

                                                                                                                                     (3, 'multiple', '以下哪些场景适合使用BFS（广度优先搜索）？',
                                                                                                                                      '[{"key":"A","value":"求无权图的最短路径"},{"key":"B","value":"判断二叉树是否为镜像"},{"key":"C","value":"求图的所有连通分量"},{"key":"D","value":"层序遍历二叉树"}]',
                                                                                                                                      '["A","D"]',
                                                                                                                                      'BFS按层扩展，天然适合最短路径（A）和层序遍历（D）；判断镜像可用DFS递归（B）；连通分量用DFS或BFS均可但通常用DFS（C）。BFS用队列，DFS用栈/递归。',
                                                                                                                                      'medium', 4, 'published', 390, 25),

                                                                                                                                     (3, 'fill', '链表中判断是否有环，常用____算法，时间复杂度O(n)，空间复杂度O(1)。',
                                                                                                                                      NULL,
                                                                                                                                      '"Floyd判圈（快慢指针）"',
                                                                                                                                      '快指针每次走2步，慢指针每次走1步，若有环则快慢指针必然相遇。也叫龟兔赛跑算法。',
                                                                                                                                      'medium', 4, 'published', 350, 22),

                                                                                                                                     (3, 'single', '堆排序的时间复杂度是？',
                                                                                                                                      '[{"key":"A","value":"O(n)"},{"key":"B","value":"O(n log n)"},{"key":"C","value":"O(n²)"},{"key":"D","value":"O(log n)"}]',
                                                                                                                                      '"B"',
                                                                                                                                      '建堆O(n)，n次取堆顶并调整O(log n)，总体O(n log n)。堆排序是原地排序，空间O(1)，但实际性能因缓存不友好通常弱于快速排序。',
                                                                                                                                      'easy', 4, 'published', 290, 18),

                                                                                                                                     (3, 'judge', '哈希表在最坏情况下查找时间复杂度为O(n)。',
                                                                                                                                      NULL,
                                                                                                                                      '"true"',
                                                                                                                                      '当所有键都哈希冲突到同一个桶时（极端情况），退化为链表，查找O(n)。优化：链地址法、开放寻址、再哈希、链表转红黑树（Java HashMap当链表长度>8时）。',
                                                                                                                                      'medium', 4, 'published', 270, 17),

                                                                                                                                     (3, 'essay', '请分析二叉搜索树（BST）和平衡二叉树（AVL）的区别及各自应用场景。',
                                                                                                                                      NULL,
                                                                                                                                      '"BST：左<根<右，查找O(h)，最坏退化O(n)（链状）；AVL：在BST基础上保证|左右子树高度差|≤1，旋转维护平衡，查找O(log n)。BST适合大量有序插入后查找少的场景；AVL查找性能稳定，但旋转开销大；工程中常用红黑树（插入删除旋转更少）如Java TreeMap。"',
                                                                                                                                      '考察树形数据结构的深度理解，涉及平衡策略和工程取舍。',
                                                                                                                                      'hard', 4, 'published', 460, 29),

-- ---- 题库4: MySQL性能优化实战 (bank_id=4, author_id=5) ----
                                                                                                                                     (4, 'single', '以下哪种情况会导致MySQL索引失效？',
                                                                                                                                      '[{"key":"A","value":"使用BETWEEN范围查询"},{"key":"B","value":"对索引列使用函数"},{"key":"C","value":"使用>=比较"},{"key":"D","value":"IS NULL查询"}]',
                                                                                                                                      '"B"',
                                                                                                                                      '对索引列使用函数（如WHERE YEAR(create_time)=2024）会导致索引失效，因为MySQL无法使用索引的有序性。应改写为范围查询。IS NULL可以走索引；BETWEEN和>=都可以走索引。',
                                                                                                                                      'medium', 5, 'published', 480, 30),

                                                                                                                                     (4, 'essay', '请解释MySQL中EXPLAIN命令各字段的含义及如何利用它进行查询优化。',
                                                                                                                                      NULL,
                                                                                                                                      '"EXPLAIN输出关键字段：type（连接类型，const>eq_ref>ref>range>index>ALL，ALL最差）；key（实际使用的索引）；rows（预估扫描行数）；Extra（Using index覆盖索引性能好；Using filesort需优化；Using temporary性能差）。优化：消除ALL类型、减少rows、利用覆盖索引。"',
                                                                                                                                      '查询优化的核心工具，必须熟练掌握。',
                                                                                                                                      'hard', 5, 'published', 520, 33),

                                                                                                                                     (4, 'judge', 'InnoDB和MyISAM都支持事务。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      'InnoDB支持事务（ACID）、行级锁、外键；MyISAM不支持事务和行级锁（只有表锁），但读取速度快，适合读多写少的场景。MySQL 5.5+默认引擎为InnoDB。',
                                                                                                                                      'easy', 5, 'published', 350, 22),

                                                                                                                                     (4, 'single', '联合索引(a, b, c)，以下哪个查询可以完整利用该索引？',
                                                                                                                                      '[{"key":"A","value":"WHERE b=1 AND c=2"},{"key":"B","value":"WHERE a=1 AND c=2"},{"key":"C","value":"WHERE a=1 AND b=2"},{"key":"D","value":"WHERE b=1"}]',
                                                                                                                                      '"C"',
                                                                                                                                      '联合索引遵循最左前缀原则：WHERE a=1 AND b=2可以使用(a,b)部分索引；WHERE a=1 AND c=2只用到a；WHERE b=1跳过a无法使用索引；WHERE a=1 AND b=2 AND c=3可完整使用。',
                                                                                                                                      'medium', 5, 'published', 440, 28),

                                                                                                                                     (4, 'multiple', '以下哪些操作可能导致MySQL死锁？',
                                                                                                                                      '[{"key":"A","value":"两个事务以不同顺序锁定多行"},{"key":"B","value":"间隙锁冲突"},{"key":"C","value":"外键约束检查"},{"key":"D","value":"SELECT ... FOR UPDATE"}]',
                                                                                                                                      '["A","B","C"]',
                                                                                                                                      'A是经典死锁场景（加锁顺序不一致）；B间隙锁在RR隔离级别下可能冲突；C外键检查会加共享锁可能导致死锁；D本身只是加排他锁，不直接导致死锁，但多个FOR UPDATE以不同顺序执行会导致死锁。',
                                                                                                                                      'hard', 5, 'published', 380, 24),

                                                                                                                                     (4, 'fill', 'MySQL中，____隔离级别可以解决幻读问题，通过MVCC和____锁实现。',
                                                                                                                                      NULL,
                                                                                                                                      '"可重复读(REPEATABLE READ)；间隙(Gap)"',
                                                                                                                                      'MySQL InnoDB的RR级别通过MVCC解决不可重复读，通过Next-Key Lock（行锁+间隙锁）解决幻读。RC级别不加间隙锁，所以存在幻读。',
                                                                                                                                      'hard', 5, 'published', 310, 20),

                                                                                                                                     (4, 'judge', 'COUNT(*)比COUNT(1)性能更差。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      'MySQL优化器对COUNT(*)和COUNT(1)处理方式相同，性能无差异。COUNT(字段名)则不同，需判断非NULL，且若字段无索引性能较差。推荐使用COUNT(*)语义更清晰。',
                                                                                                                                      'easy', 5, 'published', 290, 18),

                                                                                                                                     (4, 'single', '以下关于MySQL主从复制说法，错误的是？',
                                                                                                                                      '[{"key":"A","value":"基于binlog实现"},{"key":"B","value":"默认是异步复制"},{"key":"C","value":"从库可以进行写操作"},{"key":"D","value":"可以实现读写分离"}]',
                                                                                                                                      '"C"',
                                                                                                                                      '主从复制中，从库应配置为read_only，避免写操作导致数据不一致。A正确（基于binlog）；B正确（默认异步）；D正确（写主库、读从库）。',
                                                                                                                                      'medium', 5, 'published', 320, 20),

                                                                                                                                     (4, 'essay', '解释MySQL中的回表查询，如何通过覆盖索引优化？',
                                                                                                                                      NULL,
                                                                                                                                      '"回表：查询走非聚簇索引后，根据得到的主键值再去聚簇索引查完整数据，即两次B+树查找。覆盖索引：查询所需字段全在索引中，无需回表（EXPLAIN的Extra显示Using index）。优化：将查询的SELECT字段加入联合索引，或只SELECT索引包含的字段。"',
                                                                                                                                      '索引优化的核心概念，覆盖索引是高性能SQL的常用技巧。',
                                                                                                                                      'hard', 5, 'published', 480, 30),

                                                                                                                                     (4, 'multiple', '以下哪些是MySQL慢查询的常见原因？',
                                                                                                                                      '[{"key":"A","value":"没有合适的索引"},{"key":"B","value":"返回不必要的字段(SELECT *)"},{"key":"C","value":"大量连接（JOIN）操作"},{"key":"D","value":"数据量过大未分表分库"}]',
                                                                                                                                      '["A","B","C","D"]',
                                                                                                                                      '以上都是慢查询常见原因。A最常见；B增加IO和网络传输；C多表JOIN可能产生笛卡尔积；D单表数据量建议超过1000万考虑分表。慢查询日志（slow_query_log）是定位问题的重要工具。',
                                                                                                                                      'medium', 5, 'published', 390, 25),

-- ---- 题库5: Go语言并发编程 (bank_id=5, author_id=6) ----
                                                                                                                                     (5, 'single', 'Go中，以下哪个说法关于Goroutine是正确的？',
                                                                                                                                      '[{"key":"A","value":"Goroutine是操作系统线程"},{"key":"B","value":"创建Goroutine的初始栈大小固定为8MB"},{"key":"C","value":"Goroutine由Go运行时调度，而非OS"},{"key":"D","value":"单个Go程序最多创建1000个Goroutine"}]',
                                                                                                                                      '"C"',
                                                                                                                                      'Goroutine是用户态的轻量级协程，由Go运行时（M:N调度模型）调度，不直接对应OS线程。初始栈约2-8KB（可动态扩缩容）；数量理论上可以达到数百万。',
                                                                                                                                      'easy', 6, 'published', 260, 16),

                                                                                                                                     (5, 'judge', 'Go中，向已关闭的channel发送数据会panic。',
                                                                                                                                      NULL,
                                                                                                                                      '"true"',
                                                                                                                                      '向已关闭的channel发送数据会引发panic（send on closed channel）。从已关闭的channel接收数据不会panic，会返回零值和false。关闭channel通常由发送方负责。',
                                                                                                                                      'medium', 6, 'published', 240, 15),

                                                                                                                                     (5, 'essay', '请解释Go中的select语句的工作机制。',
                                                                                                                                      NULL,
                                                                                                                                      '"select监听多个channel操作，哪个case就绪（可发送或可接收）就执行哪个。多个case同时就绪时随机选择（避免饥饿）。没有就绪case时：有default则执行default（非阻塞）；无default则阻塞等待。常用于超时控制（结合time.After）、多路复用、非阻塞channel操作。"',
                                                                                                                                      '考察Go并发原语的核心理解。',
                                                                                                                                      'medium', 6, 'published', 310, 19),

                                                                                                                                     (5, 'single', 'Go中sync.WaitGroup的主要用途是？',
                                                                                                                                      '[{"key":"A","value":"保护共享变量"},{"key":"B","value":"等待一组Goroutine全部完成"},{"key":"C","value":"限制并发数量"},{"key":"D","value":"实现channel的广播"}]',
                                                                                                                                      '"B"',
                                                                                                                                      'WaitGroup用于等待一组goroutine完成：Add增加计数，Done减少计数（defer wg.Done()），Wait阻塞直到计数为0。保护共享变量用Mutex；限制并发用带缓冲channel或semaphore。',
                                                                                                                                      'easy', 6, 'published', 220, 14),

                                                                                                                                     (5, 'multiple', 'Go中，以下哪些方式可以安全地在多个Goroutine间共享数据？',
                                                                                                                                      '[{"key":"A","value":"sync.Mutex保护临界区"},{"key":"B","value":"通过channel传递数据"},{"key":"C","value":"sync/atomic原子操作"},{"key":"D","value":"全局变量直接读写"}]',
                                                                                                                                      '["A","B","C"]',
                                                                                                                                      'D错误，未加锁直接读写全局变量存在数据竞争。Go的并发哲学："不要通过共享内存来通信，而是通过通信来共享内存"，推荐channel方式（B）；Mutex（A）和atomic（C）是共享内存的安全方式。',
                                                                                                                                      'medium', 6, 'published', 290, 18),

                                                                                                                                     (5, 'fill', 'Go中，使用____命令可以检测程序中的数据竞争问题。',
                                                                                                                                      NULL,
                                                                                                                                      '"go run -race 或 go build -race"',
                                                                                                                                      'Go内置数据竞争检测器（Race Detector），基于ThreadSanitizer。使用-race标志编译运行，检测到竞争时会打印详细报告。会有约5-10x性能开销，生产环境不开启。',
                                                                                                                                      'easy', 6, 'published', 180, 11),

                                                                                                                                     (5, 'judge', 'Go中的map是并发安全的。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      'Go内置map不是并发安全的，并发读写会导致程序崩溃（fatal error: concurrent map read and map write）。并发场景应使用sync.Map或用Mutex保护map。',
                                                                                                                                      'easy', 6, 'published', 270, 17),

                                                                                                                                     (5, 'single', 'Go中Context主要用于解决什么问题？',
                                                                                                                                      '[{"key":"A","value":"Goroutine间传递数据"},{"key":"B","value":"Goroutine的生命周期管理和取消传播"},{"key":"C","value":"限制内存使用"},{"key":"D","value":"实现分布式锁"}]',
                                                                                                                                      '"B"',
                                                                                                                                      'Context用于：1.取消信号传播（父ctx取消，子ctx同步取消）；2.截止时间/超时设置；3.传递请求作用域的值（慎用）。解决了goroutine无法从外部被取消的问题，是Go并发模式的重要组成。',
                                                                                                                                      'medium', 6, 'published', 300, 19),

                                                                                                                                     (5, 'essay', '解释Go中GMP调度模型的各组件含义和工作原理。',
                                                                                                                                      NULL,
                                                                                                                                      '"G(Goroutine)-M(Machine/OS线程)-P(Processor/逻辑处理器)。P的数量由GOMAXPROCS设定（默认CPU核数），P持有本地Goroutine队列。M必须绑定P才能执行G。调度：M从P的本地队列取G执行，空时从全局队列或其他P偷取（Work Stealing）。G阻塞时M与P分离，P找空闲M继续工作。"',
                                                                                                                                      '深度考察Go运行时调度机制，高频面试题。',
                                                                                                                                      'hard', 6, 'published', 420, 26),

                                                                                                                                     (5, 'multiple', '以下关于Go channel的说法，正确的是？',
                                                                                                                                      '[{"key":"A","value":"无缓冲channel发送会阻塞直到有接收者"},{"key":"B","value":"有缓冲channel在缓冲区满时发送才会阻塞"},{"key":"C","value":"nil channel永远阻塞"},{"key":"D","value":"channel是引用类型"}]',
                                                                                                                                      '["A","B","C","D"]',
                                                                                                                                      '以上全部正确。nil channel的特性常被利用：在select中动态禁用某个case（将case的channel设为nil即可禁用该case而不影响其他case）。',
                                                                                                                                      'medium', 6, 'published', 310, 20),

-- ---- 题库6: TypeScript高级特性 (bank_id=6, author_id=7) ----
                                                                                                                                     (6, 'single', 'TypeScript中，以下哪种类型表示"任意类型但会进行类型检查"？',
                                                                                                                                      '[{"key":"A","value":"any"},{"key":"B","value":"unknown"},{"key":"C","value":"never"},{"key":"D","value":"void"}]',
                                                                                                                                      '"B"',
                                                                                                                                      'unknown是类型安全的any。any完全跳过类型检查；unknown接受任意值但使用时必须先缩窄类型（类型守卫或断言），保持类型安全。never表示永不存在的值；void表示无返回值。',
                                                                                                                                      'medium', 7, 'published', 290, 18),

                                                                                                                                     (6, 'essay', '请解释TypeScript中泛型的使用场景，并举例说明泛型约束的用法。',
                                                                                                                                      NULL,
                                                                                                                                      '"泛型：编写可复用的、类型参数化的代码。场景：通用容器（Array<T>）、函数复用（identity<T>(arg:T):T）、API响应类型。泛型约束：<T extends SomeType>限制T必须满足某接口，例如<T extends {length:number}>约束T有length属性，避免访问不存在的属性。"',
                                                                                                                                      '泛型是TS最重要的特性之一，考察实际应用能力。',
                                                                                                                                      'medium', 7, 'published', 340, 21),

                                                                                                                                     (6, 'judge', 'TypeScript中interface和type alias在所有情况下都可以互换使用。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      '区别：interface可以声明合并（同名interface自动合并）；type可以表示联合类型、交叉类型、元组等复杂类型；interface只能描述对象形状；type不能重复声明（同名会报错）。',
                                                                                                                                      'medium', 7, 'published', 260, 16),

                                                                                                                                     (6, 'multiple', '以下哪些是TypeScript的内置工具类型？',
                                                                                                                                      '[{"key":"A","value":"Partial<T>"},{"key":"B","value":"Required<T>"},{"key":"C","value":"Observable<T>"},{"key":"D","value":"Readonly<T>"}]',
                                                                                                                                      '["A","B","D"]',
                                                                                                                                      'TS内置工具类型：Partial（所有属性可选）、Required（所有属性必填）、Readonly（只读）、Pick、Omit、Record、Exclude、Extract等。Observable是RxJS库的类型。',
                                                                                                                                      'easy', 7, 'published', 220, 14),

                                                                                                                                     (6, 'fill', 'TypeScript中，____关键字用于在类型层面收窄联合类型，常与switch语句配合使用穷举检查。',
                                                                                                                                      NULL,
                                                                                                                                      '"never"',
                                                                                                                                      '在switch的default分支使用never类型断言，如果所有case没有穷举完，TypeScript会报错，这是一种编译时安全检查（Exhaustive Check）。',
                                                                                                                                      'hard', 7, 'published', 190, 12),

                                                                                                                                     (6, 'single', '以下TypeScript代码中，keyof typeof obj的类型是？（const obj = {a:1, b:2, c:3}）',
                                                                                                                                      '[{"key":"A","value":"string"},{"key":"B","value":"number"},{"key":"C","value":"\'a\'|\'b\'|\'c\'"},{"key":"D","value":"\'a\'&\'b\'&\'c\'"}]',
                                                                                                                                      '"C"',
                                                                                                                                      'typeof obj得到对象类型{a:number,b:number,c:number}，keyof该类型得到键的联合类型"a"|"b"|"c"。这是TypeScript类型编程的基础，常用于限制参数只能是某对象的键名。',
                                                                                                                                      'medium', 7, 'published', 270, 17),

                                                                                                                                     (6, 'judge', 'TypeScript的类型系统是结构化类型系统（鸭子类型）。',
                                                                                                                                      NULL,
                                                                                                                                      '"true"',
                                                                                                                                      '与Java的名义类型（Nominal Typing）不同，TS是结构化类型系统：只要结构兼容（有相同的属性和方法），即使名称不同也可以赋值。这更符合JavaScript的动态特性。',
                                                                                                                                      'medium', 7, 'published', 250, 16),

                                                                                                                                     (6, 'essay', '解释TypeScript中条件类型（Conditional Types）的语法和常见用法。',
                                                                                                                                      NULL,
                                                                                                                                      '"语法：T extends U ? X : Y。分布式条件类型：T为联合类型时，条件类型分发到每个成员。常见用法：NonNullable<T>（T extends null|undefined ? never : T）、ReturnType<T>（infer推断函数返回类型）、深度Partial等类型体操。配合infer可以提取类型中的子类型。"',
                                                                                                                                      '高级类型编程，考察TS深度使用能力。',
                                                                                                                                      'hard', 7, 'published', 310, 19),

                                                                                                                                     (6, 'multiple', '以下哪些是TypeScript装饰器的合法应用位置？',
                                                                                                                                      '[{"key":"A","value":"类"},{"key":"B","value":"类方法"},{"key":"C","value":"函数参数"},{"key":"D","value":"本地变量"}]',
                                                                                                                                      '["A","B","C"]',
                                                                                                                                      'TS装饰器可以应用于：类（Class Decorator）、方法（Method Decorator）、访问器（Accessor Decorator）、属性（Property Decorator）、参数（Parameter Decorator）。不能应用于本地变量（D错误）。',
                                                                                                                                      'medium', 7, 'published', 230, 14),

                                                                                                                                     (6, 'single', 'TypeScript中，以下关于枚举（enum）的说法错误的是？',
                                                                                                                                      '[{"key":"A","value":"数字枚举支持反向映射"},{"key":"B","value":"const enum在编译后会被内联"},{"key":"C","value":"字符串枚举支持反向映射"},{"key":"D","value":"枚举成员可以是计算值"}]',
                                                                                                                                      '"C"',
                                                                                                                                      '字符串枚举不支持反向映射（只有数字枚举支持，如Direction[0]可得到枚举名）。const enum编译后直接内联为字面量，不生成对象代码（B正确）。',
                                                                                                                                      'hard', 7, 'published', 210, 13),

-- ---- 题库7: Spring Boot面试宝典 (bank_id=7, author_id=8) ----
                                                                                                                                     (7, 'single', 'Spring中，@Autowired注解默认按照什么方式注入？',
                                                                                                                                      '[{"key":"A","value":"按名称（byName）"},{"key":"B","value":"按类型（byType）"},{"key":"C","value":"按构造器"},{"key":"D","value":"按Qualifier注解"}]',
                                                                                                                                      '"B"',
                                                                                                                                      '@Autowired默认byType注入。当同类型有多个Bean时报错，需配合@Qualifier指定名称，或使用@Primary标记首选Bean。推荐构造器注入（不可变、易测试）。',
                                                                                                                                      'easy', 8, 'published', 380, 24),

                                                                                                                                     (7, 'essay', '请解释Spring AOP的实现原理及其代理方式。',
                                                                                                                                      NULL,
                                                                                                                                      '"AOP通过动态代理实现横切关注点（日志、事务、权限）的模块化。Spring AOP两种代理：1.JDK动态代理（目标类实现接口时），基于反射生成接口代理；2.CGLIB代理（无接口或proxy-target-class=true），通过继承生成子类代理。SpringBoot 2.x默认CGLIB。切面核心：Pointcut（切点）、Advice（增强）、Aspect（切面=Pointcut+Advice）。"',
                                                                                                                                      'Spring核心原理，高频面试考点。',
                                                                                                                                      'hard', 8, 'published', 520, 33),

                                                                                                                                     (7, 'judge', 'Spring Boot的自动装配是通过@EnableAutoConfiguration注解实现的。',
                                                                                                                                      NULL,
                                                                                                                                      '"true"',
                                                                                                                                      '@SpringBootApplication包含@EnableAutoConfiguration，触发自动装配。原理：从classpath的META-INF/spring.factories（新版spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports）读取自动配置类，按条件注解（@ConditionalOnClass等）按需装配。',
                                                                                                                                      'medium', 8, 'published', 400, 25),

                                                                                                                                     (7, 'single', 'Spring事务中，默认情况下哪种异常会触发事务回滚？',
                                                                                                                                      '[{"key":"A","value":"所有Exception"},{"key":"B","value":"受检异常（checked exception）"},{"key":"C","value":"非受检异常（RuntimeException及其子类）"},{"key":"D","value":"所有Throwable"}]',
                                                                                                                                      '"C"',
                                                                                                                                      'Spring事务默认只回滚RuntimeException（非受检异常）和Error。若要回滚受检异常，需配置@Transactional(rollbackFor=Exception.class)。这是常见踩坑点。',
                                                                                                                                      'medium', 8, 'published', 460, 29),

                                                                                                                                     (7, 'multiple', '以下哪些是Spring Bean的作用域？',
                                                                                                                                      '[{"key":"A","value":"singleton"},{"key":"B","value":"prototype"},{"key":"C","value":"request"},{"key":"D","value":"global"}]',
                                                                                                                                      '["A","B","C"]',
                                                                                                                                      'Spring Bean作用域：singleton（默认，容器单例）、prototype（每次请求新实例）、request/session/application/websocket（Web应用）。没有"global"作用域（D错误）。',
                                                                                                                                      'easy', 8, 'published', 340, 21),

                                                                                                                                     (7, 'fill', 'Spring中，____注解用于标记一个方法在容器初始化后执行，常用于进行初始化工作。',
                                                                                                                                      NULL,
                                                                                                                                      '"@PostConstruct"',
                                                                                                                                      '@PostConstruct（JSR-250）在Bean初始化、依赖注入完成后执行，早于InitializingBean.afterPropertiesSet()。对应的@PreDestroy在销毁前执行。',
                                                                                                                                      'easy', 8, 'published', 250, 15),

                                                                                                                                     (7, 'judge', 'MyBatis中，#{}和${}都可以防止SQL注入。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      '#{}使用预编译语句（PreparedStatement），参数以?占位符替换，可防止SQL注入；${}直接字符串替换，不进行预编译，存在SQL注入风险。${}用于动态表名/列名等不能预编译的场景。',
                                                                                                                                      'easy', 8, 'published', 390, 24),

                                                                                                                                     (7, 'single', 'SpringBoot中，properties配置文件中多环境配置推荐使用哪种方式？',
                                                                                                                                      '[{"key":"A","value":"多个application.properties文件分开管理"},{"key":"B","value":"application-{profile}.yml + spring.profiles.active"},{"key":"C","value":"系统环境变量覆盖"},{"key":"D","value":"@Value注解硬编码"}]',
                                                                                                                                      '"B"',
                                                                                                                                      'SpringBoot Profile机制：application-dev.yml、application-prod.yml等，通过spring.profiles.active=prod激活指定环境。支持多Profile激活、Profile分组（spring.profiles.group）等高级特性。',
                                                                                                                                      'easy', 8, 'published', 280, 18),

                                                                                                                                     (7, 'essay', '请描述Spring IOC容器的Bean生命周期。',
                                                                                                                                      NULL,
                                                                                                                                      '"Bean生命周期：1.实例化（反射调用构造器）；2.属性填充（依赖注入）；3.Aware接口回调（setBeanName等）；4.BeanPostProcessor前置处理；5.@PostConstruct/afterPropertiesSet初始化；6.BeanPostProcessor后置处理（AOP代理在此生成）；7.使用；8.@PreDestroy/destroy销毁。"',
                                                                                                                                      'Spring面试必考题，考察对IoC容器工作机制的深度理解。',
                                                                                                                                      'hard', 8, 'published', 490, 31),

                                                                                                                                     (7, 'multiple', '以下关于@Transactional注解失效的场景，正确的是？',
                                                                                                                                      '[{"key":"A","value":"同一个类内部方法调用，被调用方法的@Transactional失效"},{"key":"B","value":"方法不是public时"},{"key":"C","value":"异常被catch后未重新抛出"},{"key":"D","value":"@Transactional标注在接口方法上（使用CGLIB代理时）"}]',
                                                                                                                                      '["A","B","C","D"]',
                                                                                                                                      '都是@Transactional的经典失效场景。A是最常见的（内部调用不经过代理）；B默认只对public方法有效；C异常被吞后无法触发回滚；D CGLIB是类代理不读取接口注解。',
                                                                                                                                      'hard', 8, 'published', 540, 34),

-- ---- 题库8: Python数据分析基础 (bank_id=8, author_id=9) ----
                                                                                                                                     (8, 'single', 'Pandas中，以下哪个方法用于查看DataFrame的基本统计信息？',
                                                                                                                                      '[{"key":"A","value":"df.info()"},{"key":"B","value":"df.describe()"},{"key":"C","value":"df.shape"},{"key":"D","value":"df.head()"}]',
                                                                                                                                      '"B"',
                                                                                                                                      'df.describe()返回数值列的统计摘要（count/mean/std/min/max/百分位数）；df.info()显示列类型和非空值数量；df.shape返回(行数,列数)元组；df.head()显示前几行。',
                                                                                                                                      'easy', 9, 'published', 230, 14),

                                                                                                                                     (8, 'essay', '请解释NumPy广播（Broadcasting）机制及其应用场景。',
                                                                                                                                      NULL,
                                                                                                                                      '"广播：不同形状的数组进行运算时，NumPy自动扩展较小数组的维度以匹配较大数组。规则：从尾部维度对齐，维度为1或不存在则可广播。应用：数组与标量运算、对每行/列减均值归一化（arr - arr.mean(axis=0)）、生成坐标网格等，避免显式循环提升性能。"',
                                                                                                                                      '向量化计算的核心概念，NumPy高效使用的关键。',
                                                                                                                                      'medium', 9, 'published', 290, 18),

                                                                                                                                     (8, 'judge', 'Pandas的loc和iloc都接受整数索引。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      'loc基于标签索引（行/列的实际名称），iloc基于位置整数索引（0,1,2...）。当行索引为整数时容易混淆：loc[1]取标签为1的行，iloc[1]取第2行（0-based）。',
                                                                                                                                      'easy', 9, 'published', 195, 12),

                                                                                                                                     (8, 'single', 'Matplotlib中，plt.show()的作用是？',
                                                                                                                                      '[{"key":"A","value":"保存图形到文件"},{"key":"B","value":"清空当前图形"},{"key":"C","value":"显示所有已创建的图形"},{"key":"D","value":"创建新的figure"}]',
                                                                                                                                      '"C"',
                                                                                                                                      'plt.show()将所有待显示的图形渲染并显示，在非交互模式下调用阻塞程序直到窗口关闭。在Jupyter中通常不需要（%matplotlib inline自动显示）。保存用plt.savefig()。',
                                                                                                                                      'easy', 9, 'published', 170, 10),

                                                                                                                                     (8, 'multiple', '以下哪些是Pandas处理缺失值的方法？',
                                                                                                                                      '[{"key":"A","value":"df.dropna()"},{"key":"B","value":"df.fillna()"},{"key":"C","value":"df.interpolate()"},{"key":"D","value":"df.remove_na()"}]',
                                                                                                                                      '["A","B","C"]',
                                                                                                                                      'dropna删除含缺失值的行/列；fillna用指定值/方法填充（如均值、前向填充ffill）；interpolate插值填充（线性、时序等）；remove_na不存在（D错误）。',
                                                                                                                                      'easy', 9, 'published', 210, 13),

                                                                                                                                     (8, 'fill', 'NumPy中，使用____函数可以将Python列表转换为NumPy数组。',
                                                                                                                                      NULL,
                                                                                                                                      '"np.array()"',
                                                                                                                                      'np.array()是最基本的创建数组方式；np.zeros()、np.ones()、np.arange()、np.linspace()等用于创建特定数组；np.asarray()在输入已是数组时不复制数据。',
                                                                                                                                      'easy', 9, 'published', 150, 9),

                                                                                                                                     (8, 'judge', 'Pandas的groupby操作会立即执行聚合计算。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      'groupby返回GroupBy对象（惰性操作），并不立即计算，只有在调用聚合函数（.sum()/.mean()/.agg()等）时才真正执行。这种惰性求值类似于SQL的GROUP BY语句。',
                                                                                                                                      'medium', 9, 'published', 200, 12),

                                                                                                                                     (8, 'single', '以下哪个NumPy操作会创建数组的视图（view）而非副本（copy）？',
                                                                                                                                      '[{"key":"A","value":"np.copy(arr)"},{"key":"B","value":"arr[1:5]（切片）"},{"key":"C","value":"arr[[0,1,2]]（花式索引）"},{"key":"D","value":"arr.astype(float)"}]',
                                                                                                                                      '"B"',
                                                                                                                                      '切片操作返回视图（修改会影响原数组），其他高级索引（花式索引、布尔索引）返回副本。np.copy()显式复制；astype()返回新数组。可用arr.base is None判断是否为视图。',
                                                                                                                                      'medium', 9, 'published', 230, 14),

                                                                                                                                     (8, 'essay', '请简述Pandas的merge和concat的区别及适用场景。',
                                                                                                                                      NULL,
                                                                                                                                      '"merge：类似SQL JOIN，按共同列/索引合并两个DataFrame（inner/left/right/outer join），适合有关联键的数据合并；concat：沿轴方向拼接，axis=0纵向堆叠（类似UNION），axis=1横向拼接，适合相同结构数据追加或特征拼接。场景区别：有共同字段关联用merge，简单堆叠/追加用concat。"',
                                                                                                                                      '数据整合的核心操作，实际数据分析中高频使用。',
                                                                                                                                      'medium', 9, 'published', 270, 17),

                                                                                                                                     (8, 'multiple', '以下哪些是数据可视化时Matplotlib的常用图表类型？',
                                                                                                                                      '[{"key":"A","value":"plt.plot()折线图"},{"key":"B","value":"plt.scatter()散点图"},{"key":"C","value":"plt.hist()直方图"},{"key":"D","value":"plt.network()网络图"}]',
                                                                                                                                      '["A","B","C"]',
                                                                                                                                      'Matplotlib常用：plot折线、scatter散点、hist直方图、bar柱状、pie饼图、boxplot箱线图、heatmap热力图（需seaborn）。网络图通常用NetworkX库（D错误）。',
                                                                                                                                      'easy', 9, 'published', 190, 12),

-- ---- 题库9: Redis缓存设计模式 (bank_id=9, author_id=10) ----
                                                                                                                                     (9, 'single', 'Redis中，以下哪种数据类型最适合实现排行榜功能？',
                                                                                                                                      '[{"key":"A","value":"String"},{"key":"B","value":"List"},{"key":"C","value":"Hash"},{"key":"D","value":"Sorted Set（ZSet）"}]',
                                                                                                                                      '"D"',
                                                                                                                                      'ZSet（有序集合）每个成员关联一个分数（score），自动按分数排序，ZRANGEBYSCORE/ZREVRANGE等命令天然支持排行榜查询，时间复杂度O(log n)。',
                                                                                                                                      'easy', 10, 'published', 310, 20),

                                                                                                                                     (9, 'essay', '请解释缓存穿透、缓存击穿、缓存雪崩的区别及解决方案。',
                                                                                                                                      NULL,
                                                                                                                                      '"穿透：查询不存在的数据，缓存不命中每次打到DB。方案：布隆过滤器（BloomFilter）拦截，或缓存空值。击穿：热点key过期，瞬间大量请求打到DB。方案：互斥锁（只一个请求重建缓存）或逻辑过期（不设TTL，异步更新）。雪崩：大量key同时过期或Redis宕机。方案：过期时间加随机偏移、集群高可用、熔断降级。"',
                                                                                                                                      'Redis缓存三大问题，面试必考，需掌握每种问题的本质和解决方案。',
                                                                                                                                      'hard', 10, 'published', 580, 37),

                                                                                                                                     (9, 'judge', 'Redis的所有操作都是原子性的。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      '单个Redis命令是原子的，但多个命令组合不是。需要原子性执行多命令时，可使用：MULTI/EXEC事务（不支持回滚）、Lua脚本（在服务端原子执行）。',
                                                                                                                                      'medium', 10, 'published', 340, 21),

                                                                                                                                     (9, 'single', 'Redis分布式锁的核心命令（推荐实现）是？',
                                                                                                                                      '[{"key":"A","value":"SETNX key value + EXPIRE key seconds"},{"key":"B","value":"SET key value NX EX seconds"},{"key":"C","value":"SETEX key seconds value"},{"key":"D","value":"GETSET key value"}]',
                                                                                                                                      '"B"',
                                                                                                                                      'SET key value NX EX seconds是原子操作（NX=不存在才设置，EX设置过期时间），避免了SETNX+EXPIRE两步操作间宕机导致死锁的问题。value应设为UUID避免误删他人的锁。',
                                                                                                                                      'medium', 10, 'published', 420, 26),

                                                                                                                                     (9, 'multiple', '以下哪些是Redis的持久化方式？',
                                                                                                                                      '[{"key":"A","value":"RDB（快照）"},{"key":"B","value":"AOF（追加日志）"},{"key":"C","value":"RDB+AOF混合持久化"},{"key":"D","value":"WAL（Write-Ahead Logging）"}]',
                                                                                                                                      '["A","B","C"]',
                                                                                                                                      'Redis持久化：RDB定期快照（体积小恢复快，可能丢数据）；AOF记录写命令日志（数据完整，文件大）；Redis 4.0+混合持久化（RDB+AOF结合优点）。WAL是PostgreSQL等关系数据库的概念，Redis不使用（D错误）。',
                                                                                                                                      'medium', 10, 'published', 350, 22),

                                                                                                                                     (9, 'fill', 'Redis中，____命令用于在ZSet中获取分数在[min,max]范围内的成员（按分数升序）。',
                                                                                                                                      NULL,
                                                                                                                                      '"ZRANGEBYSCORE"',
                                                                                                                                      'ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count]，返回有序集中分数在min到max之间的成员。Redis 6.2+推荐使用ZRANGE命令的BYSCORE参数统一替代。',
                                                                                                                                      'medium', 10, 'published', 240, 15),

                                                                                                                                     (9, 'judge', 'Redis的主从复制是同步复制，主节点写入后必须等从节点确认才返回。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      'Redis主从默认是异步复制，主节点写入后立即返回，异步将命令发给从节点。可配置wait命令等待指定数量从节点确认实现伪同步，但性能下降。完全同步需使用Redis Enterprise。',
                                                                                                                                      'medium', 10, 'published', 280, 18),

                                                                                                                                     (9, 'single', '以下哪种Redis数据类型适合存储用户签到数据（一年365天）？',
                                                                                                                                      '[{"key":"A","value":"String存储逗号分隔"},{"key":"B","value":"List存储签到日期"},{"key":"C","value":"BitMap（位图）"},{"key":"D","value":"Hash存储每天状态"}]',
                                                                                                                                      '"C"',
                                                                                                                                      'BitMap用一个bit表示一天是否签到，365天只需365bit≈46字节，极度节省内存。SETBIT/GETBIT操作O(1)，BITCOUNT统计签到次数，BITPOS找第一次签到。',
                                                                                                                                      'medium', 10, 'published', 360, 23),

                                                                                                                                     (9, 'essay', '请设计一个基于Redis的秒杀系统的核心流程。',
                                                                                                                                      NULL,
                                                                                                                                      '"1.预热：秒杀商品库存写入Redis（INCRBY stock count）；2.用户请求：DECR stock，若返回<0则无库存（原子操作防超卖）；3.成功则将订单信息放入消息队列异步处理（防DB压力）；4.用户去重：用Redis Set记录已购用户，SADD判断是否重复购买；5.前置防刷：接口限流（令牌桶/滑动窗口）+验证码。"',
                                                                                                                                      '高并发经典场景设计题，综合考察Redis多种特性。',
                                                                                                                                      'hard', 10, 'published', 490, 31),

                                                                                                                                     (9, 'multiple', '以下哪些是Redis集群模式？',
                                                                                                                                      '[{"key":"A","value":"主从复制（Master-Slave）"},{"key":"B","value":"哨兵模式（Sentinel）"},{"key":"C","value":"Cluster模式"},{"key":"D","value":"MQ模式"}]',
                                                                                                                                      '["A","B","C"]',
                                                                                                                                      '主从复制（读写分离，无自动故障转移）；哨兵（监控主从，自动故障转移，无数据分片）；Cluster（数据分片到16384个槽，支持横向扩展和自动故障转移，生产大数据量推荐）。MQ不是Redis集群模式（D错误）。',
                                                                                                                                      'medium', 10, 'published', 320, 20),

-- ---- 题库10: 前端设计模式精讲 (bank_id=10, author_id=11) ----
                                                                                                                                     (10, 'single', '以下哪个设计模式保证一个类只有一个实例？',
                                                                                                                                      '[{"key":"A","value":"工厂模式"},{"key":"B","value":"单例模式"},{"key":"C","value":"观察者模式"},{"key":"D","value":"策略模式"}]',
                                                                                                                                      '"B"',
                                                                                                                                      '单例模式：限制类的实例化，全局只有一个实例（如全局状态管理、日志记录器）。实现方式：闭包+立即执行函数、ES6 Class静态属性、模块单例（ES Module天然单例）。',
                                                                                                                                      'easy', 11, 'published', 290, 18),

                                                                                                                                     (10, 'essay', '请解释观察者模式和发布-订阅模式的区别，并举前端使用场景。',
                                                                                                                                      NULL,
                                                                                                                                      '"观察者：Subject直接持有Observer列表并通知（紧耦合），如Vue2响应系统（Dep-Watcher）。发布-订阅：Publisher和Subscriber通过EventBus/消息代理解耦（不直接引用），如Node.js EventEmitter、Vue.$emit/$on。区别：是否有中间件解耦。场景：组件通信、状态监听、DOM事件。"',
                                                                                                                                      '混淆率极高的两种模式，重点理解耦合度区别。',
                                                                                                                                      'medium', 11, 'published', 350, 22),

                                                                                                                                     (10, 'judge', '工厂模式主要用于对象的创建，将对象的创建和使用分离。',
                                                                                                                                      NULL,
                                                                                                                                      '"true"',
                                                                                                                                      '工厂模式将对象实例化的逻辑封装，调用者只需调用工厂方法而不关心具体实例化过程。简单工厂、工厂方法、抽象工厂三种形态，复杂度递增，扩展性递增。',
                                                                                                                                      'easy', 11, 'published', 220, 14),

                                                                                                                                     (10, 'single', 'React中的高阶组件（HOC）本质上使用了哪种设计模式？',
                                                                                                                                      '[{"key":"A","value":"单例模式"},{"key":"B","value":"装饰器模式"},{"key":"C","value":"代理模式"},{"key":"D","value":"策略模式"}]',
                                                                                                                                      '"B"',
                                                                                                                                      'HOC接收组件返回增强组件，是装饰器模式在React中的体现——在不修改原组件的情况下为其添加功能（日志、权限、数据获取等）。Vue中的Mixin也有类似思想。',
                                                                                                                                      'medium', 11, 'published', 310, 19),

                                                                                                                                     (10, 'multiple', '以下哪些是前端常用的设计模式？',
                                                                                                                                      '[{"key":"A","value":"MVC/MVVM"},{"key":"B","value":"代理模式（ES6 Proxy）"},{"key":"C","value":"迭代器模式（Generator）"},{"key":"D","value":"命令模式（Undo/Redo）"}]',
                                                                                                                                      '["A","B","C","D"]',
                                                                                                                                      '以上均是前端常见设计模式应用：MVVM是Vue/React的架构基础；ES6 Proxy实现响应式（Vue3）；Generator/Iterator用于控制流和异步；命令模式用于撤销重做（富文本编辑器）。',
                                                                                                                                      'medium', 11, 'published', 270, 17),

                                                                                                                                     (10, 'fill', '策略模式通过将算法封装成独立的类，使得它们可以____，从而消除大量的if-else判断。',
                                                                                                                                      NULL,
                                                                                                                                      '"互相替换（相互替换）"',
                                                                                                                                      '策略模式定义算法族，分别封装，让它们之间可以互相替换。前端场景：表单验证规则、支付方式选择、动画缓动函数等，将条件分支替换为策略对象的调用。',
                                                                                                                                      'easy', 11, 'published', 190, 12),

                                                                                                                                     (10, 'judge', '代理模式和装饰器模式的意图完全相同。',
                                                                                                                                      NULL,
                                                                                                                                      '"false"',
                                                                                                                                      '代理模式：控制对对象的访问（访问控制、缓存、延迟初始化），代理通常在内部创建被代理对象；装饰器模式：动态添加职责（功能增强），装饰器接收被装饰对象作为参数。意图不同：代理是控制访问，装饰器是增强功能。',
                                                                                                                                      'medium', 11, 'published', 250, 16),

                                                                                                                                     (10, 'single', '以下哪种模式最适合处理"一个对象的状态决定其行为"的场景？',
                                                                                                                                      '[{"key":"A","value":"策略模式"},{"key":"B","value":"命令模式"},{"key":"C","value":"状态模式"},{"key":"D","value":"模板方法模式"}]',
                                                                                                                                      '"C"',
                                                                                                                                      '状态模式：将对象的每种状态封装为独立类，状态改变时切换类，避免大量if-else。前端场景：交通灯控制、Promise状态机（pending/fulfilled/rejected）、订单状态流转。',
                                                                                                                                      'medium', 11, 'published', 280, 18),

                                                                                                                                     (10, 'essay', '请说明享元模式在前端虚拟列表中的应用思路。',
                                                                                                                                      NULL,
                                                                                                                                      '"享元模式：共享大量细粒度对象中的公共状态（内部状态），外部状态由上下文传入，减少对象创建开销。虚拟列表应用：只渲染可视区域的DOM节点（如20个），滚动时复用（共享）这些节点，修改内容（外部状态），而非为每条数据创建真实DOM（可能上万个）。React的key复用、recyclerview均为此思想。"',
                                                                                                                                      '享元模式的经典前端应用，考察对性能优化和设计模式结合理解。',
                                                                                                                                      'hard', 11, 'published', 330, 21),

                                                                                                                                     (10, 'multiple', '以下哪些属于行为型设计模式？',
                                                                                                                                      '[{"key":"A","value":"观察者模式"},{"key":"B","value":"策略模式"},{"key":"C","value":"适配器模式"},{"key":"D","value":"命令模式"}]',
                                                                                                                                      '["A","B","D"]',
                                                                                                                                      '行为型（关注对象交互）：观察者、策略、命令、状态、模板方法、迭代器、责任链等。适配器是结构型模式（关注类和对象的组合，如将旧接口适配新接口）。C错误。',
                                                                                                                                      'medium', 11, 'published', 260, 16);

-- ==============
-- 4. 用户会话数据 (5条)
-- ==============
INSERT INTO sessions (id, user_id, ip_address, user_agent, expires_at) VALUES
                                                                           ('sess_abc123def456', 2,  '192.168.1.101', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36', DATE_ADD(NOW(), INTERVAL 7 DAY)),
                                                                           ('sess_bcd234efg567', 3,  '192.168.1.102', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', DATE_ADD(NOW(), INTERVAL 7 DAY)),
                                                                           ('sess_cde345fgh678', 4,  '10.0.0.15',     'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36', DATE_ADD(NOW(), INTERVAL 7 DAY)),
                                                                           ('sess_def456ghi789', 5,  '172.16.0.33',   'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)', DATE_ADD(NOW(), INTERVAL 3 DAY)),
                                                                           ('sess_efg567hij890', 1,  '127.0.0.1',     'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36', DATE_ADD(NOW(), INTERVAL 30 DAY));

-- ==============
-- 5. 收藏数据 (10条，模拟用户收藏题库和题目)
-- ==============
INSERT INTO favorites (user_id, target_type, target_id) VALUES
                                                            (2, 'bank', 3),  -- zhang_wei 收藏 LeetCode题库
                                                            (2, 'bank', 7),  -- zhang_wei 收藏 Spring Boot题库
                                                            (3, 'bank', 6),  -- li_fang 收藏 TypeScript题库
                                                            (3, 'question', 1),  -- li_fang 收藏 Java final关键字题
                                                            (4, 'bank', 4),  -- wang_jun 收藏 MySQL题库
                                                            (4, 'question', 21), -- wang_jun 收藏 两数之和题
                                                            (5, 'bank', 9),  -- chen_xia 收藏 Redis题库
                                                            (6, 'question', 41), -- zhao_yang 收藏 Go Goroutine题
                                                            (7, 'bank', 2),  -- liu_ming 收藏 Vue3题库
                                                            (8, 'question', 71); -- sun_ling 收藏 Spring @Autowired题

-- ==============
-- 6. 点赞数据 (10条)
-- ==============
INSERT INTO likes (user_id, target_type, target_id) VALUES
                                                        (2, 'bank', 3),
                                                        (3, 'bank', 1),
                                                        (4, 'bank', 9),
                                                        (5, 'question', 32),
                                                        (6, 'question', 55),
                                                        (7, 'bank', 6),
                                                        (8, 'question', 21),
                                                        (9, 'bank', 7),
                                                        (10, 'question', 71),
                                                        (11, 'bank', 4);

-- ==============
-- 7. 关注数据 (10条，用户间互相关注)
-- ==============
INSERT INTO follows (follower_id, following_id) VALUES
                                                    (2, 3),   -- zhang_wei 关注 li_fang
                                                    (3, 2),   -- li_fang 关注 zhang_wei
                                                    (4, 2),   -- wang_jun 关注 zhang_wei
                                                    (4, 8),   -- wang_jun 关注 zhou_fei
                                                    (5, 4),   -- chen_xia 关注 wang_jun
                                                    (6, 5),   -- zhao_yang 关注 chen_xia
                                                    (7, 6),   -- liu_ming 关注 zhao_yang
                                                    (8, 7),   -- sun_ling 关注 liu_ming
                                                    (9, 10),  -- zhou_fei 关注 zheng_lan
                                                    (10, 11); -- wu_tao 关注 zheng_lan（系统uid为11的用户）

-- ==============
-- 8. 评论数据 (10条，含嵌套回复)
-- ==============
INSERT INTO comments (question_id, user_id, content, parent_id, status) VALUES
                                                                            (5,  3,  '这道题的答案解析写得很清楚！synchronized和ReentrantLock的使用场景也说明了，赞👍', NULL, 'active'),
                                                                            (5,  4,  '补充一下：ReentrantLock还支持tryLock(timeout)超时获取锁，避免死锁，实际项目中非常实用', NULL, 'active'),
                                                                            (5,  2,  '@王俊 说得对，另外Condition可以实现精准唤醒特定线程，比synchronized的notifyAll效率更高', 2, 'active'),
                                                                            (21, 5,  '两数之和是必刷题，哈希表解法确实是O(n)，但面试时别忘了讨论哈希冲突的情况', NULL, 'active'),
                                                                            (21, 6,  '这道题变形有很多：三数之和、四数之和，都可以用哈希+双指针，建议一起练', NULL, 'active'),
                                                                            (31, 7,  '联合索引最左前缀这个知识点太容易踩坑了，生产上吃过亏', NULL, 'active'),
                                                                            (31, 5,  '对，还有一个坑：WHERE a=1 AND b>1 AND c=2，c列的索引用不上，因为b用了范围查询', 6, 'active'),
                                                                            (55, 8,  'Go的GMP模型讲解得很通透，之前一直没理解Work Stealing是怎么回事，现在清楚了', NULL, 'active'),
                                                                            (71, 9,  '@Transactional的失效场景在项目里真的很坑，建议结合代码demo来学习更有效果', NULL, 'active'),
                                                                            (91, 10, '秒杀系统设计这道题很综合，Redis+消息队列+限流都需要掌握，建议搭个小项目实践一下', NULL, 'active');

-- ==============
-- 验证数据完整性
-- ==============
SELECT '=== 数据统计 ===' AS info;
SELECT '用户数' AS table_name, COUNT(*) AS count FROM users
UNION ALL SELECT '分类数', COUNT(*) FROM categories
          UNION ALL SELECT '题库数', COUNT(*) FROM banks
          UNION ALL SELECT '题目数', COUNT(*) FROM questions
          UNION ALL SELECT '收藏数', COUNT(*) FROM favorites
          UNION ALL SELECT '点赞数', COUNT(*) FROM likes
          UNION ALL SELECT '关注数', COUNT(*) FROM follows
          UNION ALL SELECT '评论数', COUNT(*) FROM comments
          UNION ALL SELECT '会话数', COUNT(*) FROM sessions;