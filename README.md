jocean-j2se
===========

jocean's J2SE util

2015-11-21: release 0.0.3 版本:
    
    1、增加 MBeanPublisher 工具类，实现监听特定MBean的创建及销毁，并以 Observable 方式暴露; 添加 MBeanPublisher & UnitConfigOnZKUpdater 对应的 Spring xml; 处理有多个 符合条件的 MBean 发布的情况
    2、根据 MBeanStatus 更新特定 ZooKeeper 上的Unit 配置信息功
    3、将 xharbor 统计实现类迁移到 jocean-j2se 中， 具体包括 10ms ~ 30s 用时区间统计类 
    4、添加stop方法, 在 zkcore 销毁时, 同时销毁所创建 功能单元
    5、UnitAgent 实现 MBeanRegisterAware，支持同一个Spring Ctx Tree中 创建的多个 UnitAgent 可以注册在不同的 ObjectName 下
    6、ZKUpdater & UnitBuilder 's executor run as foreground thread, !NOT! deamon thread
    7、实现定期检查 local.yaml 描述文件, 如有变化，则重新创建整个树形功能单元组的功能
    8、upgrade Spring Version from 4.0.6.RELEASE --> 4.2.3.RELEASE;
    9、基于 YAML 格式，在本地创建树形结构的多个功能单元, 入口 Spring 配置文件为 localbooter.xml 
    10、move DirectMmeoryIndicator from xharbor --> jocean-j2se
    11、实现基于注解的, 通过 BeanHolder 查找 bean 的自动注入机制，首先在当前 ApplicationContext 中查找匹配的 bean 实例
    12、定义 SpringBeanHolder 接口，在 BeanHolder 基础上，增加 allApplicationContext 接口方法，返回当前所有的 Spring Ctx
    13、UnitAgent 新增 allUnit 方法，获取当前所有有效的 unit 信息，包括全路径 和 Spring Ctx
    14、改变 onUnitClosed 调用时机，在 applicationContext 销毁之前调用，并修改名称为 beforeUnitClosed
    15、将 UnitListener 从 UnitAgentMXBean 分离成独立接口
    16、UnitAgent：增加 UnitListener 接口，并在UnitAgent中可设置 UnitListener 侦听实例，监控 Unit 的创建及销毁
    17、实现 SpringBeanHolder：基于 BeanFactoryAware 方式获取当前 BeanFactory, 调用BeanFactory.getBean 实现 BeanHolder.getBean 方法。
    18、UnitAgent 实现 BeanHolder 接口
    19、将部分BizMemoImpl & TIMemoImplOfRanges实现迁移到 jocean-j2se模块
    20、代码结构调整，MBeanRegister MBeanRegisterSupport MBeanRegisterAware 和
        MBeanRegisterSetter迁移到 org.jocean.j2se.jmx 子包下; UnitAdmin &
        UnitAdminMXBean 迁移到 jocean-j2se模块中的j2se.unit，并更名为 UnitAgent &
        UnitAgentMXBean，ZooKeeper相关类 ZKUpdater ZKUtils
        DefaultExhibitorRestClientWithBasicAuth 迁移到 jocean-j2se模块中的 j2se.zk 子包下
    21、从 MBeanRegisterSupport 抽象出 MBeanRegister接口，定义了 MBeanRegisterSupport
    22、添加getTextedResolvedPlaceholdersAsStringArray方法 已 String[]形式 返回占位符及对应的值
    23、BeanProxy添加新方法 setImplForced，不考虑_ref是否已经存在有效值的情况，强制设置新的实现类实例
    24、Tcp服务的应答编码可以选择使用Jackson代替FastJSON
    25、使用 gradle 构建

2014-08-20: release 0.0.2 版本：
    
    合并jocean-ext到jocean-j2se