# graylog 部署简单说明
## 环境

mongo:4.1

graylog:2.4.6-1

elasticsearch: 5.6.10
## 启动

+ 启动graylog 

      docker-compose up
  
+ 启动fluent-bit
 
   这一步需要在graylog中添加了input以后进行，而且graylog的iniput监听端口应该与fluent-bit输出端口一致
 
      docker-compose -f fluent.yml up


## 待解决

graylog显示乱码问题

解决方案：经过fluentd转发，使用fluentd的gelf插件，输入到graylog中

！！ 需要进入到fluentd容器内添加gelf插件

    gem install fluent-plugin-gelf-hs
    
    

user访问

数据retention机制

dashboard

kibana VS graylog


## 系统说明

+ 访问控制

  系统访问控制分为两个层次，role和user
  
  一个系统中有多个role，例如（admin，reader），每一个role下面有多个user。
  
  目前系统有admin和reader两个role，新建用于需要继承上面某一个role。reader对与stream和dashboard没有任何权限，所以新建role一般继承reader，在此基础上面增加权限。
  
  系统的权限以role为单位划分，每个role下的用户具有相同的权限，不能单独给用户赋予某个权限。
  
  role的权限分为两部分：stream的读写权限与dashboard的读写权限。
  
  stream的写权限主要包括管理该stream的rule。
  
  dashboard的写权限主要包括增加dashboard以及更改某个dashboard的参数。
  
  
+ 数据rotation与retention

  data rotation：
    
    系统允许某个index下的文章数目（或者大小，或者时间）超过某一阈值时候，将数据转到其他的索引上面。
    
  data retention：
    
    系统允许当某个index set中的index数目超过某一阈值以后对index采取什么行为，主要有三种：
    
    delete：删除时间早的index。
    
    close： close索引，以前的索引不再允许增加数据。
    
    do nothing： 什么都不做。
    
    archive：压缩以往数据（购买了才有这项功能）。

+ dashboard

  有一个widget cache time参数，这个参数将某一个widget的信息统计在glorylog-server中一段时间，这样就使得新加入节点不用重新计算已有的统计信息。
  
  
  在某一时间段内某个查询语句查询到的消息数量:
  
  ![](./search_result_histogram_charts.PNG)  
  
  
  某一个时间段内统计信息:
  
  ![](./statistical_value.PNG)
  
  
  并且这个图形能够显示变化趋势，并且指定上升或者下降为期望值（相应的趋势颜色会变成绿色，反之变为红色）:
  
  ![](./statistical_value_2.PNG)  
  
  
  某一个时间段内某一个字段变化，可有bar，area，line，spot四种展示方式:
  
  ![](./img/field_value_charts_bar.PNG)
  
  ![](./img/field_value_charts.PNG)
  
  ![](./img/field_value_charts_line.PNG)
  
  ![](./img/field_value_charts_spot.PNG)
  
  
  
  多个图形可以叠加:
  
  ![](./img/stacked_charts.PNG)
  
  
  
  某一段时间内某个字段出现的value的数量统计信息:
  
  ![](./img/quick_value_quick_value_results.PNG)
  
  
+ 异常值

  graylog中有stream的概念，可以新建一个stream，定义满足某个规则的为异常，查询该流变化情况，即可得到某个时间段内的异常情况。
  

   

