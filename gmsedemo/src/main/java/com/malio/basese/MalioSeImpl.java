package com.malio.basese;

/*
 * Copyright (C) 2019, Urovo Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @Date: 2021-12-24
 */
public class MalioSeImpl {

    /**
     * 打开se
     *
     * @return 状态
     */
    public static native int openSe();

    /**
     * 关闭se
     *
     * @return 状态
     */
    public static native int closeSe();

    /**
     * 获取版本号
     *
     * @return version
     */
    public static native String getSeVersion();

    /**
     * 获取版本号
     *
     * @return version
     */
    public static native String getText();

    static {
        System.loadLibrary("seapi");
    }
}
