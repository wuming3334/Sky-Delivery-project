# 苍穹外卖 Sky Take Out

## 项目简介
苍穹外卖是一款基于 Spring Boot 的外卖点餐系统，采用前后端分离架构，包含管理端和用户端双端功能。项目旨在提供高效、稳定的餐饮解决方案。

## 技术栈
- **后端框架**: Spring Boot 2.7.3
- **持久层**: MyBatis + PageHelper
- **数据库**: MySQL
- **缓存**: Redis + Spring Cache
- **安全认证**: JWT (JSON Web Token)
- **接口文档**: Knife4j (Swagger增强版)
- **其他**: Lombok, FastJSON, Druid, AspectJ, AliOSS

## 🚀 核心亮点

### 🔥 Redis 缓存应用

#### 1. 店铺营业状态实时同步
- **场景**: 管理端设置营业状态，用户端实时获取
- **实现**: 使用 Redis String 结构存储 `SHOP_STATUS`
- **优势**: 
  - 避免频繁查询数据库，显著提升响应速度
  - 确保管理端和用户端数据实时一致性
  - 支持高并发读取场景

```java
// 管理端设置状态
redisTemplate.opsForValue().set("SHOP_STATUS", status);
// 用户端获取状态
Integer status = (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");
```

#### 2. 自定义 RedisTemplate 配置
- **Key 序列化**: 采用 `StringRedisSerializer`，保证 key 的可读性
- **连接池管理**: 基于 Lettuce 连接池，支持异步非阻塞 IO
- **性能优化**: 减少序列化开销，提升缓存访问效率

```java
@Bean
public RedisTemplate redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate template = new RedisTemplate();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    return template;
}
```

---

### 🔐 权限校验与安全机制

#### 1. JWT 令牌统一认证
- **拦截器设计**: `JwtTokenAdminInterceptor` 实现 `HandlerInterceptor` 接口
- **校验流程**:
  1. 从请求头提取 token
  2. 解析 JWT 并验证签名有效性
  3. 提取用户 ID 存入 ThreadLocal
  4. 失败返回 401 状态码

```java
registry.addInterceptor(jwtTokenAdminInterceptor)
    .addPathPatterns("/admin/**")
    .excludePathPatterns("/admin/employee/login");
```

#### 2. ThreadLocal 用户上下文隔离
- **BaseContext 工具类**: 使用 `ThreadLocal` 存储当前登录用户 ID
- **线程安全**: 每个请求独立存储，避免并发冲突
- **全局访问**: Service 层可直接获取操作用户 ID

```java
// 设置与获取
BaseContext.setCurrentId(empId);
Long currentId = BaseContext.getCurrentId();
```

#### 3. 自定义注解 + AOP 自动填充
- **@AutoFill 注解**: 标识需要自动填充的方法
- **切面实现**: `AutoFillAspect` 拦截注解方法
- **自动填充字段**: 创建时间、更新时间、创建人、更新人

```java
@Around("@annotation(autoFill)")
public void autoFill(ProceedingJoinPoint joinPoint, AutoFill autoFill) {
    // 根据操作类型(INSERT/UPDATE)自动设置时间戳和操作人
}
```

#### 4. 全局异常处理
- **@RestControllerAdvice**: 统一捕获业务异常
- **权限异常**: `PermissionDeniedException` 处理未授权访问
- **SQL 约束异常**: 处理唯一键冲突等数据库异常
- **统一响应**: 所有异常返回标准化 `Result` 格式

---

### 📊 架构设计亮点

#### 1. 多模块 Maven 工程
- **sky-common**: 通用工具类、常量、异常、结果封装
- **sky-pojo**: DTO、Entity、VO 数据传输对象
- **sky-server**: 业务控制器、服务层、数据访问层

#### 2. 分层架构清晰
- **Controller**: 接收请求，参数校验，返回 Result 统一响应
- **Service**: 业务逻辑处理，事务控制 (`@Transactional`)
- **Mapper**: MyBatis 数据访问，XML 映射文件

#### 3. 分页查询标准化
- **PageHelper**: 物理分页，避免内存溢出
- **PageResult 封装**: 统一返回 total 和 records

## 项目结构
```
sky-take-out/
├── sky-common/          # 公共模块
│   ├── constant/        # 常量定义
│   ├── context/         # ThreadLocal上下文
│   ├── exception/       # 自定义异常
│   └── utils/           # 工具类(JWT、Redis等)
├── sky-pojo/            # 数据对象模块
│   ├── dto/             # 数据传输对象
│   ├── entity/          # 实体类
│   └── vo/              # 视图对象
└── sky-server/          # 业务模块
    ├── controller/      # 控制器(分admin/user)
    ├── service/         # 服务层
    ├── mapper/          # 数据访问层
    ├── interceptor/     # 拦截器
    ├── aspect/          # AOP切面
    └── config/          # 配置类
```

## 快速开始

### 环境要求
- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Redis 5.0+

### 启动项目
1. 导入 SQL 脚本到 MySQL
2. 修改 `application-dev.yml` 中的数据库和 Redis 配置
3. 运行启动类 `SkyApplication`

访问接口文档: http://localhost:8080/doc.html

## 主要功能模块

### 管理端
- ✅ 员工管理 (登录、CRUD、分页、状态管理)
- ✅ 分类管理 (菜品分类、套餐分类)
- ✅ 菜品管理 (新增含口味、分页、启停售)
- ✅ 套餐管理 (关联菜品、分页、启停售)
- ✅ 店铺管理 (营业状态设置 - Redis实现)

### 用户端
- ✅ 店铺营业状态查询 (Redis实时同步)
- 🚧 用户登录 (微信授权)
- 🚧 地址管理
- 🚧 购物车
- 🚧 订单管理
- 🚧 支付功能 (无商户号,模拟支付)
