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

import org.apache.dubbo.samples.quickstart.dubbo.api.DemoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DemoServiceImpl.sayHello 单元测试。
 * 该类不依赖 Spring 上下文，直接 new 实现类，聚焦验证核心拼接逻辑与边界行为。
 */
@DisplayName("DemoServiceImpl.sayHello 单元测试")
class DemoServiceImplTest {

    private DemoServiceImpl demoService;

    @BeforeEach
    void setUp() {
        demoService = new DemoServiceImpl();
    }

    @Test
    @DisplayName("应实现 DemoService 接口")
    void shouldImplementDemoServiceInterface() {
        assertTrue(demoService instanceof DemoService,
                "DemoServiceImpl 必须实现 DemoService 接口");
    }

    @Nested
    @DisplayName("核心拼接逻辑")
    class CoreLogic {

        @Test
        @DisplayName("普通姓名返回 'Hello ' + 姓名")
        void normalName() {
            assertEquals("Hello world", demoService.sayHello("world"));
            assertEquals("Hello 张三", demoService.sayHello("张三"));
        }

        @Test
        @DisplayName("返回结果始终以 'Hello ' 前缀开头")
        void resultStartsWithHelloPrefix() {
            String result = demoService.sayHello("dubbo");
            assertTrue(result.startsWith("Hello "),
                    "返回结果必须以 'Hello ' 开头，实际为: " + result);
        }

        @Test
        @DisplayName("返回结果不为 null")
        void resultIsNotNull() {
            assertNotNull(demoService.sayHello("any"),
                    "sayHello 不应返回 null");
        }
    }

    @Nested
    @DisplayName("边界与异常输入")
    class EdgeCases {

        @Test
        @DisplayName("空字符串返回 'Hello ' (仅前缀，无姓名)")
        void emptyName() {
            assertEquals("Hello ", demoService.sayHello(""));
        }

        @Test
        @DisplayName("纯空白姓名原样拼接，保留空白")
        void blankName() {
            String input = "   "; // 3 个空格
            // 期望 = 前缀 "Hello " 拼接原始输入，避免硬编码空格数出错
            assertEquals("Hello " + input, demoService.sayHello(input));
        }

        @Test
        @DisplayName("含前后空格的姓名保留原始空白")
        void nameWithLeadingTrailingSpaces() {
            assertEquals("Hello  abc ", demoService.sayHello(" abc "));
        }

        @Test
        @DisplayName("特殊字符与 Unicode(中文/emoji) 原样拼接")
        void specialCharacters() {
            assertEquals("Hello !@#$%^&*()", demoService.sayHello("!@#$%^&*()"));
            assertEquals("Hello 中文测试", demoService.sayHello("中文测试"));
            assertEquals("Hello 🚀", demoService.sayHello("🚀"));
        }

        @Test
        @DisplayName("null 输入按 Java 字符串拼接规则返回 'Hello null'")
        void nullName() {
            // Java 中 "Hello " + null 结果为 "Hello null"
            assertEquals("Hello null", demoService.sayHello(null));
        }
    }
}
