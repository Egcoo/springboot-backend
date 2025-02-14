
## 基础功能

- 用户模块
  - 用户注册 
  - 用户登录（账号密码） 
  - 【管理员】管理用户—增删改查 
- 题库模块 
  - 查看题库列表 
  - 查看题库详情（展示题库下的题目） 
  - 【管理员】管理题库—增删改查
- 题目模块 
  - 题目搜索 
  - 查看题目详情（进入刷题页面） 
  - 【管理员】管理题目—增删改查（比如按照题库查询题目、修改题目所属题库等）

高级功能
- 题目批量管理 
- 【管理员】操作题目—批量增删改查
  - 【管理员】批量向题库添加题目 
  - 【管理员】批量从题库移除题目 
  - 【管理员】批量删除题目 
- 分词题目搜索 
- 用户刷题记录日历图 
- 自动缓存热门题目 
- 网站流量控制和熔断 
- 动态IP黑白名单过滤 
- 同端登录冲突检测 
- 分级题目反爬虫策略



## 目录说明


#### `annotation`
**自定义注解**  
存放项目中自定义的注解类。这些注解可以用于标记特定的功能或参数，方便在代码中进行统一处理。

#### `aop`
**请求日志和权限校验**  
使用 AOP（面向切面编程）实现请求日志记录和权限校验功能。通过切面可以统一处理日志记录和权限验证，减少重复代码。

#### `common`
**通用类**  
存放项目中通用的工具类或辅助类，供其他模块使用。

#### `config`
**配置类**  
存放项目的配置类，例如 Spring 的配置、第三方库的配置等。这些类通常使用 `@Configuration` 注解。

#### `constant`
**常量**  
存放项目中使用的常量，例如状态码、枚举类型等。

#### `controller`
**控制层**  
存放控制器类（`@Controller` 或 `@RestController`），负责处理 HTTP 请求和响应，通常与前端交互。

#### `esdao`
**方便操作 ES**  
存放与 Elasticsearch（ES）相关的数据访问对象（DAO），用于操作 Elasticsearch 数据库。

#### `exception`
**异常类**  
存放项目中自定义的异常类和异常处理逻辑，例如全局异常处理器。

#### `generator`
**代码生成器**
根据resources/templates下的模板生成代码

#### `job`
**定时任务、增量备份等**  
存放定时任务相关的类，例如使用 `@Scheduled` 或 `Quartz` 实现的定时任务，以及增量备份等功能。

#### `manager`
**管理类：AI 模型、Guava 限流器等**  
存放一些管理类，区别于 service的公共代码, 和业务逻辑无关，是调用第三方sdk, 所以放在 manager中

#### `mapper`
**Mapper 用于复杂 SQL**  
存放 MyBatis 的 Mapper 接口，用于执行复杂的 SQL 操作。

#### `model`
**模型层包含：DTO、VO、Domain**  
- 数据传输对象（DTO）、
- 存放和数据库有关的entity实体类
- 视图对象（VO），返回给前端的数据模型。 
- 业务枚举（enums）,存放业务枚举。

#### `service`
**接口和实现类，经常用于注入**  
编写具体的业务逻辑，供控制器层调用。通常包含接口和实现类两部分。

#### `utils`
**工具类**  
存放项目中使用的工具类，例如字符串工具、日期工具等。

#### `wxmp`
**微信工具类**  
存放与微信相关的工具类，例如微信公众号、小程序的接口调用等。

#### `MainApplication`
**Spring Boot 启动类**  
项目的主启动类，包含 `main` 方法，用于启动 Spring Boot 应用程序。

### `src/main/resources`

#### `mapper`
存放 MyBatis 的 XML 映射文件，用于写一些自定义 SQL 语句。

#### `META-INF`
存放 Spring Boot 的自动配置文件，例如 `spring.factories`。

#### `application.yml`
**开发环境配置**  
存放开发环境的配置文件，例如数据库连接信息、端口号等。

#### `application-prod.yml`
**生产环境配置**  
存放生产环境的配置文件。

#### `application-test.yml`
**测试环境配置**  
存放测试环境的配置文件。

#### `banner.txt`
**Spring Boot 启动时显示的横幅**  
存放 Spring Boot 启动时显示的横幅内容。

#### `test.xlsx`
存放项目中使用的测试数据文件，例如 Excel 文件。

## 总结

本项目的结构清晰，按照功能模块划分，便于维护和扩展。每个包都有明确的职责，符合 Spring Boot 的最佳实践。

