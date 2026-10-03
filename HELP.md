# 家庭用药记录系统

Spring Boot 4 + MySQL 8 + JdbcTemplate，前后端页面为原生 HTML/JS，移动端自适应。

## 运行

```bash
# 开发运行
mvn spring-boot:run

# 打包后运行
mvn -DskipTests package
java -jar target/medsdata-0.0.1-SNAPSHOT.jar
```

启动后访问 http://localhost:8080/

## 数据库配置

默认连接 `localhost:3306/medsdata`，用户名/密码 `root/root`。
数据库与表需自行创建，应用不会自动初始化。可用环境变量覆盖：

```
DB_HOST  DB_PORT  DB_NAME  DB_USER  DB_PASSWORD  SERVER_PORT
```

## 页面

- `/index.html` 首页：月历 + 今日早晚服药记录
- `/history.html` 历史记录：补录 / 撤销 / 修改时间
- `/plans.html` 用药方案：版本化管理
- `/drugs.html` 药品管理
- `/stock.html` 库存与流水
- `/persons.html` 用药人管理
