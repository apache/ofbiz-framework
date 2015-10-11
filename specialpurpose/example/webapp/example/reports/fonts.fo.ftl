<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <fo:layout-master-set>
      <fo:simple-page-master master-name="portrait"
                page-width="210mm"   page-height="297mm"
                margin-top="0mm"  margin-bottom="0mm"
                margin-left="15mm" margin-right="9mm">
            <fo:region-body margin-top="19mm" margin-bottom="15mm"/>
      </fo:simple-page-master>
   </fo:layout-master-set>

   <fo:page-sequence master-reference="portrait" initial-page-number="1">
    <fo:flow flow-name="xsl-region-body">
        <fo:block text-align="center">Font samples</fo:block>
        <fo:block></fo:block>
        <fo:block color="red">Helvetica</fo:block>
        <fo:block font-family="Helvetica" font-style="normal" font-weight="normal">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block font-family="Helvetica" font-style="normal" font-weight="bold">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block font-family="Helvetica" font-style="italic" font-weight="bold">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block font-family="Helvetica" font-style="italic" font-weight="normal">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block color="red">Times</fo:block>
        <fo:block font-family="Times" font-style="normal" font-weight="normal">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block font-family="Times" font-style="normal" font-weight="bold">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block font-family="Times" font-style="italic" font-weight="bold">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block font-family="Times" font-style="italic" font-weight="normal">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block color="red">Courier</fo:block>
        <fo:block font-family="Courier" font-style="normal" font-weight="normal">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block font-family="Courier" font-style="normal" font-weight="bold">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block font-family="Courier" font-style="italic" font-weight="bold">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block font-family="Courier" font-style="italic" font-weight="normal">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block color="red">Symbol</fo:block>
        <fo:block font-family="Symbol" font-style="normal" font-weight="normal">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block color="red">Zapf Dingbats</fo:block>
        <fo:block font-family="ZapfDingbats" font-style="normal" font-weight="normal">the quick brown fox jumps over the lazy dog 1234657890</fo:block>
        <fo:block font-family="NotoSans">
                We now use the NotoSans font by default to write special characters, like: &#x20AC; &#x20B9; &#x00B0; &#x2665;
        </fo:block>
     </fo:flow>
  </fo:page-sequence>
</fo:root>
