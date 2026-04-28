package config;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.tianji.learning.utils.TableInfoContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
@Configuration
public class MybatisConfiguration {
    @Bean
    public DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor() {
        // 1. 创建空的插件实例（匹配你源码的空参构造器）
        DynamicTableNameInnerInterceptor interceptor = new DynamicTableNameInnerInterceptor();

        // 2. 设置全局表名处理器（匹配你源码的 setTableNameHandler 方法）
        interceptor.setTableNameHandler((sql, tableName) -> {
            // 只对 points_board 表生效，其他表直接返回原表名
            if ("points_board".equals(tableName)) {
                return TableInfoContext.getInfo();
            }
            return tableName;
        });

        return interceptor;
    }
}