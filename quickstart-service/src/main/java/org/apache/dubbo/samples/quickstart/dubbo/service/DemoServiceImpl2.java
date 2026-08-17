/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.samples.quickstart.dubbo.service;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.samples.quickstart.dubbo.api.DemoService;

//timeout超时时间：根据官方推荐，最佳实践是在 Provider 端配置超时时间，作为所有消费者的默认值。这样做不仅更合理，也避免了每个消费者都需要单独配置的繁琐。
// 当然，如果某个消费者有特殊需求，也可以在 Consumer 端进行覆盖配置。


@DubboService(timeout = 3000,retries = 0,version = "2.0")
public class DemoServiceImpl2 implements DemoService {

    @Override
    public String sayHello(String name) {


        return "Hello V2 " + name;
    }
}
