# 概述

binance-mgs主要提供API聚合的功能，它负责把Binance的应用的API进行组合并往外提供；

binance-mgs分为几个业务模块：

* fiatpayment：负责把法币的API进行包装往外输出，以部署包的方式提供
* account：负责把账户的API进行包装往外输出，以部署包的方式提供
* future：负责提供期货相关的业务接口
* lending：负责提供借贷、杠杠相关的业务接口
* card：负责提供card相关的业务接口
* download：负责报表的下载及其他的下载功能，以部署包的方式提供
* composite： 其他，以部署包的方式提供


# 编译

* 进入各应用模块（如：fiatpayment）

  ```
   cd binance-mgs-application
   
   mvn clean package -Dmaven.test.skip=true
  ```
* intellij下引入项目自动编译方法
  添加根目录pom.xml（+ Add as Maven Project）
  再添加binance-mgs-application具体项目的pom.xml,注意intellij下自动编译只能添加一个项目模块，添加多个项目模块只能手动编译运行。
  
# 开发流程
   * 从master分支checkout一个分支TECH-xxx/xxx  git checkout -b TECH-xxx/xxx
   * 修改代码，提pull request合并到相应模块的dev分支 解决冲突，合并代码 
      fiatpayment的dev分支为 fiatpayment-dev
      lending的dev分支为     lending-dev 
   * 测试验证
   * TECH-xxx/xxx分支提pr合并到master分支
   * 删除TECH-xxx/xxx分支
BizException ===》BusinessException

FutureException 期货异常
DownloadException 下载异常


3.该项目是cicd分离的，通过-s[service]指定具体服务：
/rebuild master -s [service]

/deploy master -e qa -s [service]

/release master -s [service] --gp

