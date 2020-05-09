package com.roncoo.eshop.inventory.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableSwagger2
public class Swagger2Conf {

    /**
     * swagger2的配置文件，这里可以配置swagger2的一些基本的内容，比如扫描的包等等
     */
    @Bean
    public Docket createRestApi() {

        // 为swagger添加header参数可供输入
        ParameterBuilder userTokenHeader = new ParameterBuilder();
        ParameterBuilder userIdHeader = new ParameterBuilder();

        return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo()).select()
                //指定扫描的包路径来定义指定要建立API的代码目录
                .apis(RequestHandlerSelectors.basePackage("com.roncoo.eshop"))
                .paths(PathSelectors.any()).build();
    }

    /**
     * 构建swagger首页展示的 api文档的信息
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                // 设置页面标题
                .title("后端api接口文档")
                // 设置联系人
                .contact(new Contact("synda", "github.com/Leme34", "syndaliang@foxmail.com"))
                // 描述
                .description("欢迎访问接口文档，这里是描述信息")
                // 定义版本号
                .version("1.0").build();
    }

}
