package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.demo.DemoService;

public class ServiceConfigExport {

    public static void main(String[] args) {
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<DemoServiceImpl>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        service.setApplication(new ApplicationConfig("demo-demo-provider"));
        service.setRegister(true);

        service.export();
    }

}
