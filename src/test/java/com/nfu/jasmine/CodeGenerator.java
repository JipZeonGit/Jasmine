package com.nfu.jasmine;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

public class CodeGenerator {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3307/jasmine"; //数据库URL
        String username = "123456"; //用户名
        String password = "123456"; //密码
        String moduleName = "cus"; //模块名
        String mapperLocation = "G:\\Code\\IDEA\\Jasmine\\Jasmine\\src\\main\\resources\\mapper\\" + moduleName; //mapper路径
        String tables = "appointment,flower,inventory,sales,vip";

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> {
                    builder.author("jipzeongit") // 设置作者
                            //.enableSwagger() // 开启 swagger 模式
                            //.fileOverride() // 覆盖已生成文件
                            .outputDir("G:\\Code\\IDEA\\Jasmine\\Jasmine\\src\\main\\java"); // 指定输出目录
                })
                .packageConfig(builder -> {
                    builder.parent("com.nfu.jasmine") // 设置父包名
                            .moduleName(moduleName) // 设置父包模块名
                            .pathInfo(Collections.singletonMap(OutputFile.xml, mapperLocation)); // 设置mapperXml生成路径
                })
                .strategyConfig(builder -> {
                    builder.addInclude(tables) // 设置需要生成的表名
                            .addTablePrefix(); // 设置过滤表前缀
                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
    }
}
