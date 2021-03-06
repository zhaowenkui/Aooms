package net.aooms.core.datasource;

import cn.hutool.core.util.StrUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import net.aooms.core.AoomsVar;
import net.aooms.core.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态数据源注册（未使用，使用DataSourceConfiguration代替）
 * Created by 风象南(yuboon) on 2018-08-18
 */
public class DynamicDataSourceRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static Logger logger = LoggerFactory.getLogger(DynamicDataSourceRegister.class);

    // 如配置文件中未指定数据源类型，使用该默认值
    private static final Object DATASOURCE_TYPE_DEFAULT = "com.zaxxer.hikari.HikariDataSource";

    // 默认数据源
    private DataSource defaultDataSource;
    // 其他数据源
    private Map<String, DataSource> moreDataSources = new HashMap<String, DataSource>();

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * 加载多数据源配置
     */
    public void setEnvironment(Environment environment) {
        initDefaultDataSource(environment);
        initMoreDataSources(environment);
    }

    /**
     * 加载主数据源配置.
     *
     * @param env
     */
    private void initDefaultDataSource(Environment env) {
        // 创建主数据源;
        defaultDataSource = buildDataSource(env,"spring.datasource.hikari", AoomsVar.DEFAULT_DATASOURCE);
    }


    /**
     * 初始化更多数据源
     */
    private void initMoreDataSources(Environment env) {
        String more = env.getProperty("spring.more-datasource.keys");
        if(StrUtil.isBlank(more)){
            return;
        }

        String[] moreDataSourceNames = more.split(",");
        for(String name : moreDataSourceNames){
            if(StrUtil.isNotBlank(name)){
                // 创建主数据源
                DataSource dataSource = buildDataSource(env,"spring.more-datasource." + name,name);
                moreDataSources.put(name, dataSource);
            }
        }


      /*  Binder binder = Binder.get(env);
        FooProperties foo = binder.bind("foo", Bindable.of(FooProperties.class)).get();

        // 读取配置文件获取更多数据源，也可以通过defaultDataSource读取数据库获取更多数据源
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "custom.datasource.");
        String dsPrefixs = propertyResolver.getProperty("names");
        for (String dsPrefix : dsPrefixs.split(",")) {// 多个数据源
            Map<String, Object> dsMap = propertyResolver.getSubProperties(dsPrefix + ".");
            DataSource ds = buildDataSource(dsMap);
            customDataSources.put(dsPrefix, ds);
            dataBinder(ds, env);
        }*/
    }

    /*
     * 创建datasource
     */
    public DataSource buildDataSource(Environment env,String prefix,String name) {
        HikariConfig config = Binder.get(env).bind(prefix, HikariConfig.class).orElse(new HikariConfig());
        // HikariDataSource dataSource = (HikariDataSource)factory.build();
        // DataSourceBuilder.create()..build()
        HikariDataSource dataSource = new HikariDataSource(config);
        System.err.println("meterRegistry = " + meterRegistry);
        dataSource.setMetricRegistry(meterRegistry);
        logger.info("DataSource [" + name + "] - Start Completed , use conifg : " + prefix);
        return dataSource;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        Map<Object, Object> targetDataSources = new HashMap<Object, Object>();

        // 将主数据源添加到更多数据源中
        targetDataSources.put(AoomsVar.DEFAULT_DATASOURCE, defaultDataSource);
        DynamicDataSourceHolder.dataSourceIds.add(AoomsVar.DEFAULT_DATASOURCE);

        // 添加更多数据源
        targetDataSources.putAll(moreDataSources);
        for (String key : moreDataSources.keySet()) {
            DynamicDataSourceHolder.dataSourceIds.add(key);
        }

        // 创建DynamicDataSource
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DynamicDataSource.class);
        beanDefinition.setSynthetic(true);
        MutablePropertyValues mpv = beanDefinition.getPropertyValues();

        // 添加属性：AbstractRoutingDataSource.defaultTargetDataSource
        mpv.addPropertyValue("defaultTargetDataSource", defaultDataSource);
        mpv.addPropertyValue("targetDataSources", targetDataSources);

        beanDefinitionRegistry.registerBeanDefinition(AoomsVar.DEFAULT_DATASOURCE, beanDefinition);
    }

}