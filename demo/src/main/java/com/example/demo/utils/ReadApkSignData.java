package com.example.demo.utils;

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
 * @Author: Felix
 * @Date: 2020-12-11
 */

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ReadApkSignData {
    private final ArrayList<BlockInfo> blockInfos;

    /**
     * the struct that describe each block of the apk file
     * normal apk with v2 sign block: |Contents of ZIP entries|--|APK signing Block|--|Central Directory|--|End of Central Directory|
     * citic apk with v2 sign block and sgn data main body: |Contents of ZIP entries|--|APK signing Block|--|Central Directory|--|End of Central Directory|--|SGN data main body|
     * function: to read 1024 block of the apk file to verify
     *
     **/
    static class BlockInfo {
        private final long offset;
        private final long blockLength;
        private final RandomAccessFile raf;
        private final ByteBuffer data;

        public BlockInfo(long offset,long blockLength,RandomAccessFile raf) {
            this.offset = offset;
            this.blockLength = blockLength;
            this.raf = raf;
            this.data = null;
        }

        public BlockInfo(long offset,long blockLength,ByteBuffer data) {
            this.offset = offset;
            this.blockLength = blockLength;
            this.data = data;
            this.raf = null;
        }

        /**
         * To read "1024 block" of the apk file,every time read 1024 bytes
         * @param offset the offset of the block("Contents of ZIP entries","APK signing Block","Central Directory","End of Central Directory","SGN data main body") that need to read
         * @param len the length that the buffer need to read from the block("Contents of ZIP entries","APK signing Block","Central Directory","End of Central Directory","SGN data main body")
         * @param buffer save the data that read from the block("Contents of ZIP entries","APK signing Block","Central Directory","End of Central Directory","SGN data main body")
         * @param bufferOffset the offset of the buffer(means the start location of the buffer)
         *
         * @return void
         *
         **/
        public void read(long offset,long len,byte[] buffer,long bufferOffset) {
            if (raf == null) {
                this.data.position((int)offset);
                this.data.get(buffer,(int)bufferOffset,(int)len);
            } else {
                try {
                    this.raf.seek(offset);
                    this.raf.read(buffer, (int)bufferOffset, (int)len);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ReadApkSignData(long cozeLength, long asbLength,long cdLength,long eocdLength,
                                long sgnLength,RandomAccessFile apkFile, ByteBuffer asbData,
                                ByteBuffer cdData, ByteBuffer eocdData, ByteBuffer sgnData) {
        blockInfos = new ArrayList<>();

        // init Contents of ZIP entries block
        BlockInfo blockInfo = new BlockInfo(0, cozeLength, apkFile);
        blockInfos.add(blockInfo);

        // init APK Signing Block without sgn data
        if (asbLength != 0) {
            blockInfo = new BlockInfo(cozeLength, asbLength, asbData);
            blockInfos.add(blockInfo);
        }

        // init Central Directory block
        blockInfo = new BlockInfo(cozeLength + asbLength, cdLength, cdData);
        blockInfos.add(blockInfo);

        // init End of Central Directory
        blockInfo = new BlockInfo(cozeLength + asbLength + cdLength, eocdLength, eocdData);
        blockInfos.add(blockInfo);

        // init sgn Data
        blockInfo = new BlockInfo(cozeLength + asbLength + cdLength + eocdLength, sgnLength, sgnData);
        blockInfos.add(blockInfo);
    }


    public long readBlock(long offset,long length,byte[] buffer) {
        long bufferReaded = 0; // the length that buffer has read
        long needToRead = 0; // the length that buffer need to read
        int begin = findInList(blockInfos,offset);
        int end = findInList(blockInfos,length + offset);
        for (int i = begin; i <= end; i++) {
            if (i == blockInfos.size()) {
                break;
            }
            BlockInfo blockInfo = blockInfos.get(i);
            if (begin == end) {
                needToRead = length;
                blockInfo.read(offset - blockInfo.offset,needToRead,buffer,0);
            } else if (i == begin) {
                needToRead = blockInfo.blockLength - (offset - blockInfo.offset);
                blockInfo.read(offset - blockInfo.offset,needToRead,buffer,0);
            } else if (i == end) {
                needToRead = length - bufferReaded;
                blockInfo.read(0,needToRead,buffer,bufferReaded);
            } else {
                blockInfo.read(0,blockInfo.blockLength,buffer,bufferReaded);
                needToRead = blockInfo.blockLength;
            }
            bufferReaded += needToRead;
        }
        return bufferReaded;
    }

    // use to find the offset and the end tag in which block("Contents of ZIP entries","APK signing Block",
    // "Central Directory","End of Central Directory","SGN data main body")
    private int findInList(ArrayList<BlockInfo> list,long offset) {
        int i;
        for (i = 0;i < list.size();i++) {
            long start = list.get(i).offset;
            long length = list.get(i).offset + list.get(i).blockLength;
            if (offset <= length && offset >= start) {
                break;
            }
        }
        return i;
    }
}
