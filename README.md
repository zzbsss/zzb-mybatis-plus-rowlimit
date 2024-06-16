## zzb-mybatis-plus-rowlimit 组件
> zzb-mybatis-plus-rowlimit 是一个基于mybatis plus 插件机制开发的一个sql预检组件，能够有效防止由于开发人员疏忽导致java内存溢出，对于大表进行sql预检，超过一定数量时候进行阻断

### 目前已实现功能

1.支持多数据源

2.支持配置指定表检查

3.支持配置指定数据源检查

4.支持配置自定义最大支持查询量

### 使用
引入依赖
``` xml
    <dependency>
            <groupId>io.github.zzbsss</groupId>
            <artifactId>zzb-mybatis-plus-rowlimit</artifactId>
            <version>1.0-SNAPSHOT</version>
    </dependency>
``` 

### 应用配置
``` properties
# 是否启用组件
zzb.mp.enabled=true
# 可查询最大行数 默认 50000
zzb.mp.max-row-count=50000
# 当未配置指定表时，针对整表大小超过配置大小的表名进行监控 默认10万
zzb.mp.check-table-size=100000
# 需要监控的表名
zzb.mp.check-tables[0]=collect_web
zzb.mp.check-tables[1]=collect_config
# 需要数据源别名
zzb.mp.check-data-sources[0]=master
# 更新监控表 corn 默认每天凌晨1点更新
zzb.mp.exec-cron="0 0 1 * * ?"
```

### Q&A
1.多数据源有相同表名怎么配置，需要配置监控表名，怎么配置
配置监控表名时带上数据源名称 例如 databasename.tablename

