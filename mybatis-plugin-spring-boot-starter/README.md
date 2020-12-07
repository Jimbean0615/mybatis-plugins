# 介绍

## 1. 现有功能点

* 提供SQL自动打印功能
* 提供SQL自动追加traceid前缀功能，完善zeye分布式调用链路
* 提供最大查询数量限制

## 2. 参数配置

* 参数配置前缀：mybatis.plugin
* mybatis.plugin.limits：配置MySQL查询最大条数限制
* mybatis.plugin.autoAddTraceid：配置SQL语句是否自动追加traceid前缀
* mybatis.plugin.autoLogSQL：配置是否自动输出SQL日志到日志文件

## 3. 示例如下配置：

```properties
mybatis:
  plugin:
    limits: //SQL查询最大条数限制
      RequestRecordDao1: //配置key，全局唯一，建议采用“Dao类名+数字”格式
        stmtid: com.jimbean.communication.dao.RequestRecordDao.getUndo //SQL  Map Statement id
        autoSetup: true //是否开始追加limit，默认值true
        limitValue: 1000 //限制查询最大条数的具体值，默认1000
      RequestRecordDao2:
        stmtid: cn.gov.zcy.communication.dao.VoiceRecordDao.countStatus
    autoAddTraceid: true //是否开启SQL语句自动追加traceid前缀，默认值false
    autoLogSQL: false //是否开启SQL自动打印功能，默认值false，配置为true将在log日志中输出SQL语句
```
