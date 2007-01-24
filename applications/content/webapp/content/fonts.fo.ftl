<?xml version="1.0" encoding="UTF-8"?>
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
<#escape x as x?xml>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

  <!-- defines the layout master -->
  <fo:layout-master-set>
    <fo:simple-page-master master-name="first"
                           page-height="29.7cm"
                           page-width="21cm"
                           margin-top="1cm"
                           margin-bottom="2cm"
                           margin-left="2.5cm"
                           margin-right="2.5cm">
      <fo:region-body margin-top="1cm"/>
      <fo:region-before extent="1cm"/>
      <fo:region-after extent="1.5cm"/>
    </fo:simple-page-master>
  </fo:layout-master-set>

  <!-- starts actual layout -->
  <fo:page-sequence master-reference="first">


<fo:flow flow-name="xsl-region-body">

  <fo:block font-family="Helvetica" font-size="14pt">
Helvetica
  </fo:block>
  <fo:block space-after.optimum="10pt" font-family="Helvetica" font-size="10pt">
<fo:table>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-body>
<fo:table-row>
<fo:table-cell>
  <fo:block>
&amp;#x21; : &#x21;
&amp;#x22; : &#x22;
&amp;#x23; : &#x23;
&amp;#x24; : &#x24;
&amp;#x25; : &#x25;
&amp;#x26; : &#x26;
&amp;#x27; : &#x27;
&amp;#x28; : &#x28;
&amp;#x29; : &#x29;
&amp;#x2A; : &#x2A;
&amp;#x2B; : &#x2B;
&amp;#x2C; : &#x2C;
&amp;#x2D; : &#x2D;
&amp;#x2E; : &#x2E;
&amp;#x2F; : &#x2F;
&amp;#x30; : &#x30;
&amp;#x31; : &#x31;
&amp;#x32; : &#x32;
&amp;#x33; : &#x33;
&amp;#x34; : &#x34;
&amp;#x35; : &#x35;
&amp;#x36; : &#x36;
&amp;#x37; : &#x37;
&amp;#x38; : &#x38;
&amp;#x39; : &#x39;
&amp;#x3A; : &#x3A;
&amp;#x3B; : &#x3B;
&amp;#x3C; : &#x3C;
&amp;#x3D; : &#x3D;
&amp;#x3E; : &#x3E;
&amp;#x3F; : &#x3F;
&amp;#x40; : &#x40;
&amp;#x41; : &#x41;
&amp;#x42; : &#x42;
&amp;#x43; : &#x43;
&amp;#x44; : &#x44;
&amp;#x45; : &#x45;
&amp;#x46; : &#x46;
&amp;#x47; : &#x47;
&amp;#x48; : &#x48;
&amp;#x49; : &#x49;
&amp;#x4A; : &#x4A;
&amp;#x4B; : &#x4B;
&amp;#x4C; : &#x4C;
&amp;#x4D; : &#x4D;
&amp;#x4E; : &#x4E;
&amp;#x4F; : &#x4F;
&amp;#x50; : &#x50;
&amp;#x51; : &#x51;
&amp;#x52; : &#x52;
&amp;#x53; : &#x53;
&amp;#x54; : &#x54;
&amp;#x55; : &#x55;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
&amp;#x56; : &#x56;
&amp;#x57; : &#x57;
&amp;#x58; : &#x58;
&amp;#x59; : &#x59;
&amp;#x5A; : &#x5A;
&amp;#x5B; : &#x5B;
&amp;#x5C; : &#x5C;
&amp;#x5D; : &#x5D;
&amp;#x5E; : &#x5E;
&amp;#x5F; : &#x5F;
&amp;#x60; : &#x60;
&amp;#x61; : &#x61;
&amp;#x62; : &#x62;
&amp;#x63; : &#x63;
&amp;#x64; : &#x64;
&amp;#x65; : &#x65;
&amp;#x66; : &#x66;
&amp;#x67; : &#x67;
&amp;#x68; : &#x68;
&amp;#x69; : &#x69;
&amp;#x6A; : &#x6A;
&amp;#x6B; : &#x6B;
&amp;#x6C; : &#x6C;
&amp;#x6D; : &#x6D;
&amp;#x6E; : &#x6E;
&amp;#x6F; : &#x6F;
&amp;#x70; : &#x70;
&amp;#x71; : &#x71;
&amp;#x72; : &#x72;
&amp;#x73; : &#x73;
&amp;#x74; : &#x74;
&amp;#x75; : &#x75;
&amp;#x76; : &#x76;
&amp;#x77; : &#x77;
&amp;#x78; : &#x78;
&amp;#x79; : &#x79;
&amp;#x7A; : &#x7A;
&amp;#x7B; : &#x7B;
&amp;#x7C; : &#x7C;
&amp;#x7D; : &#x7D;
&amp;#x7E; : &#x7E;
&amp;#xA1; : &#xA1;
&amp;#xA2; : &#xA2;
&amp;#xA3; : &#xA3;
&amp;#xA4; : &#xA4;
&amp;#xA5; : &#xA5;
&amp;#xA6; : &#xA6;
&amp;#xA7; : &#xA7;
&amp;#xA8; : &#xA8;
&amp;#xA9; : &#xA9;
&amp;#xAA; : &#xAA;
&amp;#xAB; : &#xAB;
&amp;#xAC; : &#xAC;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
&amp;#xAE; : &#xAE;
&amp;#xAF; : &#xAF;
&amp;#xB0; : &#xB0;
&amp;#xB1; : &#xB1;
&amp;#xB2; : &#xB2;
&amp;#xB3; : &#xB3;
&amp;#xB4; : &#xB4;
&amp;#xB5; : &#xB5;
&amp;#xB6; : &#xB6;
&amp;#xB7; : &#xB7;
&amp;#xB8; : &#xB8;
&amp;#xB9; : &#xB9;
&amp;#xBA; : &#xBA;
&amp;#xBB; : &#xBB;
&amp;#xBC; : &#xBC;
&amp;#xBD; : &#xBD;
&amp;#xBE; : &#xBE;
&amp;#xBF; : &#xBF;
&amp;#xC0; : &#xC0;
&amp;#xC1; : &#xC1;
&amp;#xC2; : &#xC2;
&amp;#xC3; : &#xC3;
&amp;#xC4; : &#xC4;
&amp;#xC5; : &#xC5;
&amp;#xC6; : &#xC6;
&amp;#xC7; : &#xC7;
&amp;#xC8; : &#xC8;
&amp;#xC9; : &#xC9;
&amp;#xCA; : &#xCA;
&amp;#xCB; : &#xCB;
&amp;#xCC; : &#xCC;
&amp;#xCD; : &#xCD;
&amp;#xCE; : &#xCE;
&amp;#xCF; : &#xCF;
&amp;#xD0; : &#xD0;
&amp;#xD1; : &#xD1;
&amp;#xD2; : &#xD2;
&amp;#xD3; : &#xD3;
&amp;#xD4; : &#xD4;
&amp;#xD5; : &#xD5;
&amp;#xD6; : &#xD6;
&amp;#xD7; : &#xD7;
&amp;#xD8; : &#xD8;
&amp;#xD9; : &#xD9;
&amp;#xDA; : &#xDA;
&amp;#xDB; : &#xDB;
&amp;#xDC; : &#xDC;
&amp;#xDD; : &#xDD;
&amp;#xDE; : &#xDE;
&amp;#xDF; : &#xDF;
&amp;#xE0; : &#xE0;
&amp;#xE1; : &#xE1;
&amp;#xE2; : &#xE2;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
&amp;#xE3; : &#xE3;
&amp;#xE4; : &#xE4;
&amp;#xE5; : &#xE5;
&amp;#xE6; : &#xE6;
&amp;#xE7; : &#xE7;
&amp;#xE8; : &#xE8;
&amp;#xE9; : &#xE9;
&amp;#xEA; : &#xEA;
&amp;#xEB; : &#xEB;
&amp;#xEC; : &#xEC;
&amp;#xED; : &#xED;
&amp;#xEE; : &#xEE;
&amp;#xEF; : &#xEF;
&amp;#xF0; : &#xF0;
&amp;#xF1; : &#xF1;
&amp;#xF2; : &#xF2;
&amp;#xF3; : &#xF3;
&amp;#xF4; : &#xF4;
&amp;#xF5; : &#xF5;
&amp;#xF6; : &#xF6;
&amp;#xF7; : &#xF7;
&amp;#xF8; : &#xF8;
&amp;#xF9; : &#xF9;
&amp;#xFA; : &#xFA;
&amp;#xFB; : &#xFB;
&amp;#xFC; : &#xFC;
&amp;#xFD; : &#xFD;
&amp;#xFE; : &#xFE;
&amp;#xFF; : &#xFF;
&amp;#x0152; : &#x0152;
&amp;#x0153; : &#x0153;
&amp;#x0160; : &#x0160;
&amp;#x0161; : &#x0161;
&amp;#x0178; : &#x0178;
&amp;#x017D; : &#x017D;
&amp;#x017E; : &#x017E;
&amp;#x0192; : &#x0192;
&amp;#x02DC; : &#x02DC;
&amp;#x2013; : &#x2013;
&amp;#x2014; : &#x2014;
&amp;#x2018; : &#x2018;
&amp;#x2019; : &#x2019;
&amp;#x201A; : &#x201A;
&amp;#x201C; : &#x201C;
&amp;#x201D; : &#x201D;
&amp;#x201E; : &#x201E;
&amp;#x2020; : &#x2020;
&amp;#x2021; : &#x2021;
&amp;#x2022; : &#x2022;
&amp;#x2026; : &#x2026;
&amp;#x2030; : &#x2030;
&amp;#x2039; : &#x2039;
&amp;#x203A; : &#x203A;
&amp;#x2122; : &#x2122;
  </fo:block>
</fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
  </fo:block>

  <fo:block font-family="Helvetica"  font-size="14pt">
Times Roman
  </fo:block>
  <fo:block space-after.optimum="10pt" font-family="Times Roman" font-size="10pt">
<fo:table> 
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-body>
<fo:table-row>
<fo:table-cell>
  <fo:block> 
&amp;#x21; : &#x21;
&amp;#x22; : &#x22;
&amp;#x23; : &#x23;
&amp;#x24; : &#x24;
&amp;#x25; : &#x25;
&amp;#x26; : &#x26;
&amp;#x27; : &#x27;
&amp;#x28; : &#x28;
&amp;#x29; : &#x29;
&amp;#x2A; : &#x2A;
&amp;#x2B; : &#x2B;
&amp;#x2C; : &#x2C;
&amp;#x2D; : &#x2D;
&amp;#x2E; : &#x2E;
&amp;#x2F; : &#x2F;
&amp;#x30; : &#x30;
&amp;#x31; : &#x31;
&amp;#x32; : &#x32;
&amp;#x33; : &#x33;
&amp;#x34; : &#x34;
&amp;#x35; : &#x35;
&amp;#x36; : &#x36;
&amp;#x37; : &#x37;
&amp;#x38; : &#x38;
&amp;#x39; : &#x39;
&amp;#x3A; : &#x3A;
&amp;#x3B; : &#x3B;
&amp;#x3C; : &#x3C;
&amp;#x3D; : &#x3D;
&amp;#x3E; : &#x3E;
&amp;#x3F; : &#x3F;
&amp;#x40; : &#x40;
&amp;#x41; : &#x41;
&amp;#x42; : &#x42;
&amp;#x43; : &#x43;
&amp;#x44; : &#x44;
&amp;#x45; : &#x45;
&amp;#x46; : &#x46;
&amp;#x47; : &#x47;
&amp;#x48; : &#x48;
&amp;#x49; : &#x49;
&amp;#x4A; : &#x4A;
&amp;#x4B; : &#x4B;
&amp;#x4C; : &#x4C;
&amp;#x4D; : &#x4D;
&amp;#x4E; : &#x4E;
&amp;#x4F; : &#x4F;
&amp;#x50; : &#x50;
&amp;#x51; : &#x51;
&amp;#x52; : &#x52;
&amp;#x53; : &#x53;
&amp;#x54; : &#x54;
&amp;#x55; : &#x55;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
&amp;#x56; : &#x56;
&amp;#x57; : &#x57;
&amp;#x58; : &#x58;
&amp;#x59; : &#x59;
&amp;#x5A; : &#x5A;
&amp;#x5B; : &#x5B;
&amp;#x5C; : &#x5C;
&amp;#x5D; : &#x5D;
&amp;#x5E; : &#x5E;
&amp;#x5F; : &#x5F;
&amp;#x60; : &#x60;
&amp;#x61; : &#x61;
&amp;#x62; : &#x62;
&amp;#x63; : &#x63;
&amp;#x64; : &#x64;
&amp;#x65; : &#x65;
&amp;#x66; : &#x66;
&amp;#x67; : &#x67;
&amp;#x68; : &#x68;
&amp;#x69; : &#x69;
&amp;#x6A; : &#x6A;
&amp;#x6B; : &#x6B;
&amp;#x6C; : &#x6C;
&amp;#x6D; : &#x6D;
&amp;#x6E; : &#x6E;
&amp;#x6F; : &#x6F;
&amp;#x70; : &#x70;
&amp;#x71; : &#x71;
&amp;#x72; : &#x72;
&amp;#x73; : &#x73;
&amp;#x74; : &#x74;
&amp;#x75; : &#x75;
&amp;#x76; : &#x76;
&amp;#x77; : &#x77;
&amp;#x78; : &#x78;
&amp;#x79; : &#x79;
&amp;#x7A; : &#x7A;
&amp;#x7B; : &#x7B;
&amp;#x7C; : &#x7C;
&amp;#x7D; : &#x7D;
&amp;#x7E; : &#x7E;
&amp;#xA1; : &#xA1;
&amp;#xA2; : &#xA2;
&amp;#xA3; : &#xA3;
&amp;#xA4; : &#xA4;
&amp;#xA5; : &#xA5;
&amp;#xA6; : &#xA6;
&amp;#xA7; : &#xA7;
&amp;#xA8; : &#xA8;
&amp;#xA9; : &#xA9;
&amp;#xAA; : &#xAA;
&amp;#xAB; : &#xAB;
&amp;#xAC; : &#xAC;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
&amp;#xAE; : &#xAE;
&amp;#xAF; : &#xAF;
&amp;#xB0; : &#xB0;
&amp;#xB1; : &#xB1;
&amp;#xB2; : &#xB2;
&amp;#xB3; : &#xB3;
&amp;#xB4; : &#xB4;
&amp;#xB5; : &#xB5;
&amp;#xB6; : &#xB6;
&amp;#xB7; : &#xB7;
&amp;#xB8; : &#xB8;
&amp;#xB9; : &#xB9;
&amp;#xBA; : &#xBA;
&amp;#xBB; : &#xBB;
&amp;#xBC; : &#xBC;
&amp;#xBD; : &#xBD;
&amp;#xBE; : &#xBE;
&amp;#xBF; : &#xBF;
&amp;#xC0; : &#xC0;
&amp;#xC1; : &#xC1;
&amp;#xC2; : &#xC2;
&amp;#xC3; : &#xC3;
&amp;#xC4; : &#xC4;
&amp;#xC5; : &#xC5;
&amp;#xC6; : &#xC6;
&amp;#xC7; : &#xC7;
&amp;#xC8; : &#xC8;
&amp;#xC9; : &#xC9;
&amp;#xCA; : &#xCA;
&amp;#xCB; : &#xCB;
&amp;#xCC; : &#xCC;
&amp;#xCD; : &#xCD;
&amp;#xCE; : &#xCE;
&amp;#xCF; : &#xCF;
&amp;#xD0; : &#xD0;
&amp;#xD1; : &#xD1;
&amp;#xD2; : &#xD2;
&amp;#xD3; : &#xD3;
&amp;#xD4; : &#xD4;
&amp;#xD5; : &#xD5;
&amp;#xD6; : &#xD6;
&amp;#xD7; : &#xD7;
&amp;#xD8; : &#xD8;
&amp;#xD9; : &#xD9;
&amp;#xDA; : &#xDA;
&amp;#xDB; : &#xDB;
&amp;#xDC; : &#xDC;
&amp;#xDD; : &#xDD;
&amp;#xDE; : &#xDE;
&amp;#xDF; : &#xDF;
&amp;#xE0; : &#xE0;
&amp;#xE1; : &#xE1;
&amp;#xE2; : &#xE2;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
&amp;#xE3; : &#xE3;
&amp;#xE4; : &#xE4;
&amp;#xE5; : &#xE5;
&amp;#xE6; : &#xE6;
&amp;#xE7; : &#xE7;
&amp;#xE8; : &#xE8;
&amp;#xE9; : &#xE9;
&amp;#xEA; : &#xEA;
&amp;#xEB; : &#xEB;
&amp;#xEC; : &#xEC;
&amp;#xED; : &#xED;
&amp;#xEE; : &#xEE;
&amp;#xEF; : &#xEF;
&amp;#xF0; : &#xF0;
&amp;#xF1; : &#xF1;
&amp;#xF2; : &#xF2;
&amp;#xF3; : &#xF3;
&amp;#xF4; : &#xF4;
&amp;#xF5; : &#xF5;
&amp;#xF6; : &#xF6;
&amp;#xF7; : &#xF7;
&amp;#xF8; : &#xF8;
&amp;#xF9; : &#xF9;
&amp;#xFA; : &#xFA;
&amp;#xFB; : &#xFB;
&amp;#xFC; : &#xFC;
&amp;#xFD; : &#xFD;
&amp;#xFE; : &#xFE;
&amp;#xFF; : &#xFF;
&amp;#x0152; : &#x0152;
&amp;#x0153; : &#x0153;
&amp;#x0160; : &#x0160;
&amp;#x0161; : &#x0161;
&amp;#x0178; : &#x0178;
&amp;#x017D; : &#x017D;
&amp;#x017E; : &#x017E;
&amp;#x0192; : &#x0192;
&amp;#x02DC; : &#x02DC;
&amp;#x2013; : &#x2013;
&amp;#x2014; : &#x2014;
&amp;#x2018; : &#x2018;
&amp;#x2019; : &#x2019;
&amp;#x201A; : &#x201A;
&amp;#x201C; : &#x201C;
&amp;#x201D; : &#x201D;
&amp;#x201E; : &#x201E;
&amp;#x2020; : &#x2020;
&amp;#x2021; : &#x2021;
&amp;#x2022; : &#x2022;
&amp;#x2026; : &#x2026;
&amp;#x2030; : &#x2030;
&amp;#x2039; : &#x2039;
&amp;#x203A; : &#x203A;
&amp;#x2122; : &#x2122;
  </fo:block>
</fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
  </fo:block>

  <fo:block font-family="Helvetica"  font-size="14pt">
Courier
  </fo:block>
  <fo:block space-after.optimum="10pt" font-family="Courier" font-size="10pt">
<fo:table>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="75pt"/>
<fo:table-body>
<fo:table-row>
<fo:table-cell>
  <fo:block>
&amp;#x21; : &#x21;
&amp;#x22; : &#x22;
&amp;#x23; : &#x23;
&amp;#x24; : &#x24;
&amp;#x25; : &#x25;
&amp;#x26; : &#x26;
&amp;#x27; : &#x27;
&amp;#x28; : &#x28;
&amp;#x29; : &#x29;
&amp;#x2A; : &#x2A;
&amp;#x2B; : &#x2B;
&amp;#x2C; : &#x2C;
&amp;#x2D; : &#x2D;
&amp;#x2E; : &#x2E;
&amp;#x2F; : &#x2F;
&amp;#x30; : &#x30;
&amp;#x31; : &#x31;
&amp;#x32; : &#x32;
&amp;#x33; : &#x33;
&amp;#x34; : &#x34;
&amp;#x35; : &#x35;
&amp;#x36; : &#x36;
&amp;#x37; : &#x37;
&amp;#x38; : &#x38;
&amp;#x39; : &#x39;
&amp;#x3A; : &#x3A;
&amp;#x3B; : &#x3B;
&amp;#x3C; : &#x3C;
&amp;#x3D; : &#x3D;
&amp;#x3E; : &#x3E;
&amp;#x3F; : &#x3F;
&amp;#x40; : &#x40;
&amp;#x41; : &#x41;
&amp;#x42; : &#x42;
&amp;#x43; : &#x43;
&amp;#x44; : &#x44;
&amp;#x45; : &#x45;
&amp;#x46; : &#x46;
&amp;#x47; : &#x47;
&amp;#x48; : &#x48;
&amp;#x49; : &#x49;
&amp;#x4A; : &#x4A;
&amp;#x4B; : &#x4B;
&amp;#x4C; : &#x4C;
&amp;#x4D; : &#x4D;
&amp;#x4E; : &#x4E;
&amp;#x4F; : &#x4F;
&amp;#x50; : &#x50;
&amp;#x51; : &#x51;
&amp;#x52; : &#x52;
&amp;#x53; : &#x53;
&amp;#x54; : &#x54;
&amp;#x55; : &#x55;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
&amp;#x56; : &#x56;
&amp;#x57; : &#x57;
&amp;#x58; : &#x58;
&amp;#x59; : &#x59;
&amp;#x5A; : &#x5A;
&amp;#x5B; : &#x5B;
&amp;#x5C; : &#x5C;
&amp;#x5D; : &#x5D;
&amp;#x5E; : &#x5E;
&amp;#x5F; : &#x5F;
&amp;#x60; : &#x60;
&amp;#x61; : &#x61;
&amp;#x62; : &#x62;
&amp;#x63; : &#x63;
&amp;#x64; : &#x64;
&amp;#x65; : &#x65;
&amp;#x66; : &#x66;
&amp;#x67; : &#x67;
&amp;#x68; : &#x68;
&amp;#x69; : &#x69;
&amp;#x6A; : &#x6A;
&amp;#x6B; : &#x6B;
&amp;#x6C; : &#x6C;
&amp;#x6D; : &#x6D;
&amp;#x6E; : &#x6E;
&amp;#x6F; : &#x6F;
&amp;#x70; : &#x70;
&amp;#x71; : &#x71;
&amp;#x72; : &#x72;
&amp;#x73; : &#x73;
&amp;#x74; : &#x74;
&amp;#x75; : &#x75;
&amp;#x76; : &#x76;
&amp;#x77; : &#x77;
&amp;#x78; : &#x78;
&amp;#x79; : &#x79;
&amp;#x7A; : &#x7A;
&amp;#x7B; : &#x7B;
&amp;#x7C; : &#x7C;
&amp;#x7D; : &#x7D;
&amp;#x7E; : &#x7E;
&amp;#xA1; : &#xA1;
&amp;#xA2; : &#xA2;
&amp;#xA3; : &#xA3;
&amp;#xA4; : &#xA4;
&amp;#xA5; : &#xA5;
&amp;#xA6; : &#xA6;
&amp;#xA7; : &#xA7;
&amp;#xA8; : &#xA8;
&amp;#xA9; : &#xA9;
&amp;#xAA; : &#xAA;
&amp;#xAB; : &#xAB;
&amp;#xAC; : &#xAC;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
&amp;#xAE; : &#xAE;
&amp;#xAF; : &#xAF;
&amp;#xB0; : &#xB0;
&amp;#xB1; : &#xB1;
&amp;#xB2; : &#xB2;
&amp;#xB3; : &#xB3;
&amp;#xB4; : &#xB4;
&amp;#xB5; : &#xB5;
&amp;#xB6; : &#xB6;
&amp;#xB7; : &#xB7;
&amp;#xB8; : &#xB8;
&amp;#xB9; : &#xB9;
&amp;#xBA; : &#xBA;
&amp;#xBB; : &#xBB;
&amp;#xBC; : &#xBC;
&amp;#xBD; : &#xBD;
&amp;#xBE; : &#xBE;
&amp;#xBF; : &#xBF;
&amp;#xC0; : &#xC0;
&amp;#xC1; : &#xC1;
&amp;#xC2; : &#xC2;
&amp;#xC3; : &#xC3;
&amp;#xC4; : &#xC4;
&amp;#xC5; : &#xC5;
&amp;#xC6; : &#xC6;
&amp;#xC7; : &#xC7;
&amp;#xC8; : &#xC8;
&amp;#xC9; : &#xC9;
&amp;#xCA; : &#xCA;
&amp;#xCB; : &#xCB;
&amp;#xCC; : &#xCC;
&amp;#xCD; : &#xCD;
&amp;#xCE; : &#xCE;
&amp;#xCF; : &#xCF;
&amp;#xD0; : &#xD0;
&amp;#xD1; : &#xD1;
&amp;#xD2; : &#xD2;
&amp;#xD3; : &#xD3;
&amp;#xD4; : &#xD4;
&amp;#xD5; : &#xD5;
&amp;#xD6; : &#xD6;
&amp;#xD7; : &#xD7;
&amp;#xD8; : &#xD8;
&amp;#xD9; : &#xD9;
&amp;#xDA; : &#xDA;
&amp;#xDB; : &#xDB;
&amp;#xDC; : &#xDC;
&amp;#xDD; : &#xDD;
&amp;#xDE; : &#xDE;
&amp;#xDF; : &#xDF;
&amp;#xE0; : &#xE0;
&amp;#xE1; : &#xE1;
&amp;#xE2; : &#xE2;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
&amp;#xE3; : &#xE3;
&amp;#xE4; : &#xE4;
&amp;#xE5; : &#xE5;
&amp;#xE6; : &#xE6;
&amp;#xE7; : &#xE7;
&amp;#xE8; : &#xE8;
&amp;#xE9; : &#xE9;
&amp;#xEA; : &#xEA;
&amp;#xEB; : &#xEB;
&amp;#xEC; : &#xEC;
&amp;#xED; : &#xED;
&amp;#xEE; : &#xEE;
&amp;#xEF; : &#xEF;
&amp;#xF0; : &#xF0;
&amp;#xF1; : &#xF1;
&amp;#xF2; : &#xF2;
&amp;#xF3; : &#xF3;
&amp;#xF4; : &#xF4;
&amp;#xF5; : &#xF5;
&amp;#xF6; : &#xF6;
&amp;#xF7; : &#xF7;
&amp;#xF8; : &#xF8;
&amp;#xF9; : &#xF9;
&amp;#xFA; : &#xFA;
&amp;#xFB; : &#xFB;
&amp;#xFC; : &#xFC;
&amp;#xFD; : &#xFD;
&amp;#xFE; : &#xFE;
&amp;#xFF; : &#xFF;
&amp;#x0152; : &#x0152;
&amp;#x0153; : &#x0153;
&amp;#x0160; : &#x0160;
&amp;#x0161; : &#x0161;
&amp;#x0178; : &#x0178;
&amp;#x017D; : &#x017D;
&amp;#x017E; : &#x017E;
&amp;#x0192; : &#x0192;
&amp;#x02DC; : &#x02DC;
&amp;#x2013; : &#x2013;
&amp;#x2014; : &#x2014;
&amp;#x2018; : &#x2018;
&amp;#x2019; : &#x2019;
&amp;#x201A; : &#x201A;
&amp;#x201C; : &#x201C;
&amp;#x201D; : &#x201D;
&amp;#x201E; : &#x201E;
&amp;#x2020; : &#x2020;
&amp;#x2021; : &#x2021;
&amp;#x2022; : &#x2022;
&amp;#x2026; : &#x2026;
&amp;#x2030; : &#x2030;
&amp;#x2039; : &#x2039;
&amp;#x203A; : &#x203A;
&amp;#x2122; : &#x2122;
  </fo:block>
</fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
  </fo:block>

  <fo:block font-family="Helvetica"  font-size="14pt">
 ZapfDingbats:
  </fo:block>
  <fo:block space-after.optimum="10pt" font-family="ZapfDingbats" font-size="10pt">
<fo:table>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="75pt"/>
<fo:table-body>
<fo:table-row>
<fo:table-cell>
  <fo:block>
<fo:inline font-family="Helvetica">&amp;#x2701; :</fo:inline> &#x2701;
<fo:inline font-family="Helvetica">&amp;#x2702; :</fo:inline> &#x2702;
<fo:inline font-family="Helvetica">&amp;#x2703; :</fo:inline> &#x2703;
<fo:inline font-family="Helvetica">&amp;#x2704; :</fo:inline> &#x2704;
<fo:inline font-family="Helvetica">&amp;#x260E; :</fo:inline> &#x260E;
<fo:inline font-family="Helvetica">&amp;#x2706; :</fo:inline> &#x2706;
<fo:inline font-family="Helvetica">&amp;#x2707; :</fo:inline> &#x2707;
<fo:inline font-family="Helvetica">&amp;#x2708; :</fo:inline> &#x2708;
<fo:inline font-family="Helvetica">&amp;#x2709; :</fo:inline> &#x2709;
<fo:inline font-family="Helvetica">&amp;#x261B; :</fo:inline> &#x261B;
<fo:inline font-family="Helvetica">&amp;#x261E; :</fo:inline> &#x261E;
<fo:inline font-family="Helvetica">&amp;#x270C; :</fo:inline> &#x270C;
<fo:inline font-family="Helvetica">&amp;#x270D; :</fo:inline> &#x270D;
<fo:inline font-family="Helvetica">&amp;#x270E; :</fo:inline> &#x270E;
<fo:inline font-family="Helvetica">&amp;#x270F; :</fo:inline> &#x270F;
<fo:inline font-family="Helvetica">&amp;#x2710; :</fo:inline> &#x2710;
<fo:inline font-family="Helvetica">&amp;#x2711; :</fo:inline> &#x2711;
<fo:inline font-family="Helvetica">&amp;#x2712; :</fo:inline> &#x2712;
<fo:inline font-family="Helvetica">&amp;#x2713; :</fo:inline> &#x2713;
<fo:inline font-family="Helvetica">&amp;#x2714; :</fo:inline> &#x2714;
<fo:inline font-family="Helvetica">&amp;#x2715; :</fo:inline> &#x2715;
<fo:inline font-family="Helvetica">&amp;#x2716; :</fo:inline> &#x2716;
<fo:inline font-family="Helvetica">&amp;#x2717; :</fo:inline> &#x2717;
<fo:inline font-family="Helvetica">&amp;#x2718; :</fo:inline> &#x2718;
<fo:inline font-family="Helvetica">&amp;#x2719; :</fo:inline> &#x2719;
<fo:inline font-family="Helvetica">&amp;#x271A; :</fo:inline> &#x271A;
<fo:inline font-family="Helvetica">&amp;#x271B; :</fo:inline> &#x271B;
<fo:inline font-family="Helvetica">&amp;#x271C; :</fo:inline> &#x271C;
<fo:inline font-family="Helvetica">&amp;#x271D; :</fo:inline> &#x271D;
<fo:inline font-family="Helvetica">&amp;#x271E; :</fo:inline> &#x271E;
<fo:inline font-family="Helvetica">&amp;#x271F; :</fo:inline> &#x271F;
<fo:inline font-family="Helvetica">&amp;#x2720; :</fo:inline> &#x2720;
<fo:inline font-family="Helvetica">&amp;#x2721; :</fo:inline> &#x2721;
<fo:inline font-family="Helvetica">&amp;#x2722; :</fo:inline> &#x2722;
<fo:inline font-family="Helvetica">&amp;#x2723; :</fo:inline> &#x2723;
<fo:inline font-family="Helvetica">&amp;#x2724; :</fo:inline> &#x2724;
<fo:inline font-family="Helvetica">&amp;#x2725; :</fo:inline> &#x2725;
<fo:inline font-family="Helvetica">&amp;#x2726; :</fo:inline> &#x2726;
<fo:inline font-family="Helvetica">&amp;#x2727; :</fo:inline> &#x2727;
<fo:inline font-family="Helvetica">&amp;#x2605; :</fo:inline> &#x2605;
<fo:inline font-family="Helvetica">&amp;#x2729; :</fo:inline> &#x2729;
<fo:inline font-family="Helvetica">&amp;#x272A; :</fo:inline> &#x272A;
<fo:inline font-family="Helvetica">&amp;#x272B; :</fo:inline> &#x272B;
<fo:inline font-family="Helvetica">&amp;#x272C; :</fo:inline> &#x272C;
<fo:inline font-family="Helvetica">&amp;#x272D; :</fo:inline> &#x272D;
<fo:inline font-family="Helvetica">&amp;#x272E; :</fo:inline> &#x272E;
<fo:inline font-family="Helvetica">&amp;#x272F; :</fo:inline> &#x272F;
<fo:inline font-family="Helvetica">&amp;#x2730; :</fo:inline> &#x2730;
<fo:inline font-family="Helvetica">&amp;#x2731; :</fo:inline> &#x2731;
<fo:inline font-family="Helvetica">&amp;#x2732; :</fo:inline> &#x2732;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
<fo:inline font-family="Helvetica">&amp;#x2733; :</fo:inline> &#x2733;
<fo:inline font-family="Helvetica">&amp;#x2734; :</fo:inline> &#x2734;
<fo:inline font-family="Helvetica">&amp;#x2735; :</fo:inline> &#x2735;
<fo:inline font-family="Helvetica">&amp;#x2736; :</fo:inline> &#x2736;
<fo:inline font-family="Helvetica">&amp;#x2737; :</fo:inline> &#x2737;
<fo:inline font-family="Helvetica">&amp;#x2738; :</fo:inline> &#x2738;
<fo:inline font-family="Helvetica">&amp;#x2739; :</fo:inline> &#x2739;
<fo:inline font-family="Helvetica">&amp;#x273A; :</fo:inline> &#x273A;
<fo:inline font-family="Helvetica">&amp;#x273B; :</fo:inline> &#x273B;
<fo:inline font-family="Helvetica">&amp;#x273C; :</fo:inline> &#x273C;
<fo:inline font-family="Helvetica">&amp;#x273D; :</fo:inline> &#x273D;
<fo:inline font-family="Helvetica">&amp;#x273E; :</fo:inline> &#x273E;
<fo:inline font-family="Helvetica">&amp;#x273F; :</fo:inline> &#x273F;
<fo:inline font-family="Helvetica">&amp;#x2740; :</fo:inline> &#x2740;
<fo:inline font-family="Helvetica">&amp;#x2741; :</fo:inline> &#x2741;
<fo:inline font-family="Helvetica">&amp;#x2742; :</fo:inline> &#x2742;
<fo:inline font-family="Helvetica">&amp;#x2743; :</fo:inline> &#x2743;
<fo:inline font-family="Helvetica">&amp;#x2744; :</fo:inline> &#x2744;
<fo:inline font-family="Helvetica">&amp;#x2745; :</fo:inline> &#x2745;
<fo:inline font-family="Helvetica">&amp;#x2746; :</fo:inline> &#x2746;
<fo:inline font-family="Helvetica">&amp;#x2747; :</fo:inline> &#x2747;
<fo:inline font-family="Helvetica">&amp;#x2748; :</fo:inline> &#x2748;
<fo:inline font-family="Helvetica">&amp;#x2749; :</fo:inline> &#x2749;
<fo:inline font-family="Helvetica">&amp;#x274A; :</fo:inline> &#x274A;
<fo:inline font-family="Helvetica">&amp;#x274B; :</fo:inline> &#x274B;
<fo:inline font-family="Helvetica">&amp;#x25CF; :</fo:inline> &#x25CF;
<fo:inline font-family="Helvetica">&amp;#x274D; :</fo:inline> &#x274D;
<fo:inline font-family="Helvetica">&amp;#x25A0; :</fo:inline> &#x25A0;
<fo:inline font-family="Helvetica">&amp;#x274F; :</fo:inline> &#x274F;
<fo:inline font-family="Helvetica">&amp;#x2750; :</fo:inline> &#x2750;
<fo:inline font-family="Helvetica">&amp;#x2751; :</fo:inline> &#x2751;
<fo:inline font-family="Helvetica">&amp;#x2752; :</fo:inline> &#x2752;
<fo:inline font-family="Helvetica">&amp;#x25B2; :</fo:inline> &#x25B2;
<fo:inline font-family="Helvetica">&amp;#x25BC; :</fo:inline> &#x25BC;
<fo:inline font-family="Helvetica">&amp;#x25C6; :</fo:inline> &#x25C6;
<fo:inline font-family="Helvetica">&amp;#x2756; :</fo:inline> &#x2756;
<fo:inline font-family="Helvetica">&amp;#x25D7; :</fo:inline> &#x25D7;
<fo:inline font-family="Helvetica">&amp;#x2758; :</fo:inline> &#x2758;
<fo:inline font-family="Helvetica">&amp;#x2759; :</fo:inline> &#x2759;
<fo:inline font-family="Helvetica">&amp;#x275A; :</fo:inline> &#x275A;
<fo:inline font-family="Helvetica">&amp;#x275B; :</fo:inline> &#x275B;
<fo:inline font-family="Helvetica">&amp;#x275C; :</fo:inline> &#x275C;
<fo:inline font-family="Helvetica">&amp;#x275D; :</fo:inline> &#x275D;
<fo:inline font-family="Helvetica">&amp;#x275E; :</fo:inline> &#x275E;
<fo:inline font-family="Helvetica">&amp;#x2761; :</fo:inline> &#x2761;
<fo:inline font-family="Helvetica">&amp;#x2762; :</fo:inline> &#x2762;
<fo:inline font-family="Helvetica">&amp;#x2763; :</fo:inline> &#x2763;
<fo:inline font-family="Helvetica">&amp;#x2764; :</fo:inline> &#x2764;
<fo:inline font-family="Helvetica">&amp;#x2765; :</fo:inline> &#x2765;
<fo:inline font-family="Helvetica">&amp;#x2766; :</fo:inline> &#x2766;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
<fo:inline font-family="Helvetica">&amp;#x2767; :</fo:inline> &#x2767;
<fo:inline font-family="Helvetica">&amp;#x2663; :</fo:inline> &#x2663;
<fo:inline font-family="Helvetica">&amp;#x2666; :</fo:inline> &#x2666;
<fo:inline font-family="Helvetica">&amp;#x2665; :</fo:inline> &#x2665;
<fo:inline font-family="Helvetica">&amp;#x2660; :</fo:inline> &#x2660;
<fo:inline font-family="Helvetica">&amp;#x2460; :</fo:inline> &#x2460;
<fo:inline font-family="Helvetica">&amp;#x2461; :</fo:inline> &#x2461;
<fo:inline font-family="Helvetica">&amp;#x2462; :</fo:inline> &#x2462;
<fo:inline font-family="Helvetica">&amp;#x2463; :</fo:inline> &#x2463;
<fo:inline font-family="Helvetica">&amp;#x2464; :</fo:inline> &#x2464;
<fo:inline font-family="Helvetica">&amp;#x2465; :</fo:inline> &#x2465;
<fo:inline font-family="Helvetica">&amp;#x2466; :</fo:inline> &#x2466;
<fo:inline font-family="Helvetica">&amp;#x2467; :</fo:inline> &#x2467;
<fo:inline font-family="Helvetica">&amp;#x2468; :</fo:inline> &#x2468;
<fo:inline font-family="Helvetica">&amp;#x2469; :</fo:inline> &#x2469;
<fo:inline font-family="Helvetica">&amp;#x2776; :</fo:inline> &#x2776;
<fo:inline font-family="Helvetica">&amp;#x2777; :</fo:inline> &#x2777;
<fo:inline font-family="Helvetica">&amp;#x2778; :</fo:inline> &#x2778;
<fo:inline font-family="Helvetica">&amp;#x2779; :</fo:inline> &#x2779;
<fo:inline font-family="Helvetica">&amp;#x277A; :</fo:inline> &#x277A;
<fo:inline font-family="Helvetica">&amp;#x277B; :</fo:inline> &#x277B;
<fo:inline font-family="Helvetica">&amp;#x277C; :</fo:inline> &#x277C;
<fo:inline font-family="Helvetica">&amp;#x277D; :</fo:inline> &#x277D;
<fo:inline font-family="Helvetica">&amp;#x277E; :</fo:inline> &#x277E;
<fo:inline font-family="Helvetica">&amp;#x277F; :</fo:inline> &#x277F;
<fo:inline font-family="Helvetica">&amp;#x2780; :</fo:inline> &#x2780;
<fo:inline font-family="Helvetica">&amp;#x2781; :</fo:inline> &#x2781;
<fo:inline font-family="Helvetica">&amp;#x2782; :</fo:inline> &#x2782;
<fo:inline font-family="Helvetica">&amp;#x2783; :</fo:inline> &#x2783;
<fo:inline font-family="Helvetica">&amp;#x2784; :</fo:inline> &#x2784;
<fo:inline font-family="Helvetica">&amp;#x2785; :</fo:inline> &#x2785;
<fo:inline font-family="Helvetica">&amp;#x2786; :</fo:inline> &#x2786;
<fo:inline font-family="Helvetica">&amp;#x2787; :</fo:inline> &#x2787;
<fo:inline font-family="Helvetica">&amp;#x2788; :</fo:inline> &#x2788;
<fo:inline font-family="Helvetica">&amp;#x2789; :</fo:inline> &#x2789;
<fo:inline font-family="Helvetica">&amp;#x278A; :</fo:inline> &#x278A;
<fo:inline font-family="Helvetica">&amp;#x278B; :</fo:inline> &#x278B;
<fo:inline font-family="Helvetica">&amp;#x278C; :</fo:inline> &#x278C;
<fo:inline font-family="Helvetica">&amp;#x278D; :</fo:inline> &#x278D;
<fo:inline font-family="Helvetica">&amp;#x278E; :</fo:inline> &#x278E;
<fo:inline font-family="Helvetica">&amp;#x278F; :</fo:inline> &#x278F;
<fo:inline font-family="Helvetica">&amp;#x2790; :</fo:inline> &#x2790;
<fo:inline font-family="Helvetica">&amp;#x2791; :</fo:inline> &#x2791;
<fo:inline font-family="Helvetica">&amp;#x2792; :</fo:inline> &#x2792;
<fo:inline font-family="Helvetica">&amp;#x2793; :</fo:inline> &#x2793;
<fo:inline font-family="Helvetica">&amp;#x2794; :</fo:inline> &#x2794;
<fo:inline font-family="Helvetica">&amp;#x2192; :</fo:inline> &#x2192;
<fo:inline font-family="Helvetica">&amp;#x2194; :</fo:inline> &#x2194;
<fo:inline font-family="Helvetica">&amp;#x2195; :</fo:inline> &#x2195;
<fo:inline font-family="Helvetica">&amp;#x2798; :</fo:inline> &#x2798;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
<fo:inline font-family="Helvetica">&amp;#x2799; :</fo:inline> &#x2799;
<fo:inline font-family="Helvetica">&amp;#x279A; :</fo:inline> &#x279A;
<fo:inline font-family="Helvetica">&amp;#x279B; :</fo:inline> &#x279B;
<fo:inline font-family="Helvetica">&amp;#x279C; :</fo:inline> &#x279C;
<fo:inline font-family="Helvetica">&amp;#x279D; :</fo:inline> &#x279D;
<fo:inline font-family="Helvetica">&amp;#x279E; :</fo:inline> &#x279E;
<fo:inline font-family="Helvetica">&amp;#x279F; :</fo:inline> &#x279F;
<fo:inline font-family="Helvetica">&amp;#x27A0; :</fo:inline> &#x27A0;
<fo:inline font-family="Helvetica">&amp;#x27A1; :</fo:inline> &#x27A1;
<fo:inline font-family="Helvetica">&amp;#x27A2; :</fo:inline> &#x27A2;
<fo:inline font-family="Helvetica">&amp;#x27A3; :</fo:inline> &#x27A3;
<fo:inline font-family="Helvetica">&amp;#x27A4; :</fo:inline> &#x27A4;
<fo:inline font-family="Helvetica">&amp;#x27A5; :</fo:inline> &#x27A5;
<fo:inline font-family="Helvetica">&amp;#x27A6; :</fo:inline> &#x27A6;
<fo:inline font-family="Helvetica">&amp;#x27A7; :</fo:inline> &#x27A7;
<fo:inline font-family="Helvetica">&amp;#x27A8; :</fo:inline> &#x27A8;
<fo:inline font-family="Helvetica">&amp;#x27A9; :</fo:inline> &#x27A9;
<fo:inline font-family="Helvetica">&amp;#x27AA; :</fo:inline> &#x27AA;
<fo:inline font-family="Helvetica">&amp;#x27AB; :</fo:inline> &#x27AB;
<fo:inline font-family="Helvetica">&amp;#x27AC; :</fo:inline> &#x27AC;
<fo:inline font-family="Helvetica">&amp;#x27AD; :</fo:inline> &#x27AD;
<fo:inline font-family="Helvetica">&amp;#x27AE; :</fo:inline> &#x27AE;
<fo:inline font-family="Helvetica">&amp;#x27AF; :</fo:inline> &#x27AF;
<fo:inline font-family="Helvetica">&amp;#x27B1; :</fo:inline> &#x27B1;
<fo:inline font-family="Helvetica">&amp;#x27B2; :</fo:inline> &#x27B2;
<fo:inline font-family="Helvetica">&amp;#x27B3; :</fo:inline> &#x27B3;
<fo:inline font-family="Helvetica">&amp;#x27B4; :</fo:inline> &#x27B4;
<fo:inline font-family="Helvetica">&amp;#x27B5; :</fo:inline> &#x27B5;
<fo:inline font-family="Helvetica">&amp;#x27B6; :</fo:inline> &#x27B6;
<fo:inline font-family="Helvetica">&amp;#x27B7; :</fo:inline> &#x27B7;
<fo:inline font-family="Helvetica">&amp;#x27B8; :</fo:inline> &#x27B8;
<fo:inline font-family="Helvetica">&amp;#x27B9; :</fo:inline> &#x27B9;
<fo:inline font-family="Helvetica">&amp;#x27BA; :</fo:inline> &#x27BA;
<fo:inline font-family="Helvetica">&amp;#x27BB; :</fo:inline> &#x27BB;
<fo:inline font-family="Helvetica">&amp;#x27BC; :</fo:inline> &#x27BC;
<fo:inline font-family="Helvetica">&amp;#x27BD; :</fo:inline> &#x27BD;
<fo:inline font-family="Helvetica">&amp;#x27BE; :</fo:inline> &#x27BE;
<fo:inline font-family="Helvetica">&amp;#xF8E0; :</fo:inline> &#xF8E0;
<fo:inline font-family="Helvetica">&amp;#xF8DE; :</fo:inline> &#xF8DE;
<fo:inline font-family="Helvetica">&amp;#xF8E3; :</fo:inline> &#xF8E3;
<fo:inline font-family="Helvetica">&amp;#xF8DD; :</fo:inline> &#xF8DD;
<fo:inline font-family="Helvetica">&amp;#xF8D7; :</fo:inline> &#xF8D7;
<fo:inline font-family="Helvetica">&amp;#xF8E1; :</fo:inline> &#xF8E1;
<fo:inline font-family="Helvetica">&amp;#xF8DB; :</fo:inline> &#xF8DB;
<fo:inline font-family="Helvetica">&amp;#xF8D8; :</fo:inline> &#xF8D8;
<fo:inline font-family="Helvetica">&amp;#xF8DF; :</fo:inline> &#xF8DF;
<fo:inline font-family="Helvetica">&amp;#xF8DA; :</fo:inline> &#xF8DA;
<fo:inline font-family="Helvetica">&amp;#xF8D9; :</fo:inline> &#xF8D9;
<fo:inline font-family="Helvetica">&amp;#xF8DC; :</fo:inline> &#xF8DC;
<fo:inline font-family="Helvetica">&amp;#xF8E4; :</fo:inline> &#xF8E4;
<fo:inline font-family="Helvetica">&amp;#xF8E2; :</fo:inline> &#xF8E2;
  </fo:block>
</fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
  </fo:block>

  <fo:block font-family="Helvetica"  font-size="14pt">
 Symbol:
  </fo:block>
  <fo:block space-after.optimum="10pt" font-family="Symbol" font-size="10pt">
<fo:table>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="65pt"/>
<fo:table-column column-width="30pt"/>
<fo:table-column column-width="75pt"/>
<fo:table-body>
<fo:table-row>
<fo:table-cell>
  <fo:block>
<fo:inline font-family="Helvetica">&amp;#x21; :</fo:inline> &#x21;
<fo:inline font-family="Helvetica">&amp;#x23; :</fo:inline> &#x23;
<fo:inline font-family="Helvetica">&amp;#x25; :</fo:inline> &#x25;
<fo:inline font-family="Helvetica">&amp;#x26; :</fo:inline> &#x26;
<fo:inline font-family="Helvetica">&amp;#x28; :</fo:inline> &#x28;
<fo:inline font-family="Helvetica">&amp;#x29; :</fo:inline> &#x29;
<fo:inline font-family="Helvetica">&amp;#x2B; :</fo:inline> &#x2B;
<fo:inline font-family="Helvetica">&amp;#x2C; :</fo:inline> &#x2C;
<fo:inline font-family="Helvetica">&amp;#x2E; :</fo:inline> &#x2E;
<fo:inline font-family="Helvetica">&amp;#x2F; :</fo:inline> &#x2F;
<fo:inline font-family="Helvetica">&amp;#x30; :</fo:inline> &#x30;
<fo:inline font-family="Helvetica">&amp;#x31; :</fo:inline> &#x31;
<fo:inline font-family="Helvetica">&amp;#x32; :</fo:inline> &#x32;
<fo:inline font-family="Helvetica">&amp;#x33; :</fo:inline> &#x33;
<fo:inline font-family="Helvetica">&amp;#x34; :</fo:inline> &#x34;
<fo:inline font-family="Helvetica">&amp;#x35; :</fo:inline> &#x35;
<fo:inline font-family="Helvetica">&amp;#x36; :</fo:inline> &#x36;
<fo:inline font-family="Helvetica">&amp;#x37; :</fo:inline> &#x37;
<fo:inline font-family="Helvetica">&amp;#x38; :</fo:inline> &#x38;
<fo:inline font-family="Helvetica">&amp;#x39; :</fo:inline> &#x39;
<fo:inline font-family="Helvetica">&amp;#x3A; :</fo:inline> &#x3A;
<fo:inline font-family="Helvetica">&amp;#x3B; :</fo:inline> &#x3B;
<fo:inline font-family="Helvetica">&amp;#x3C; :</fo:inline> &#x3C;
<fo:inline font-family="Helvetica">&amp;#x3D; :</fo:inline> &#x3D;
<fo:inline font-family="Helvetica">&amp;#x3E; :</fo:inline> &#x3E;
<fo:inline font-family="Helvetica">&amp;#x3F; :</fo:inline> &#x3F;
<fo:inline font-family="Helvetica">&amp;#x5B; :</fo:inline> &#x5B;
<fo:inline font-family="Helvetica">&amp;#x5D; :</fo:inline> &#x5D;
<fo:inline font-family="Helvetica">&amp;#x5F; :</fo:inline> &#x5F;
<fo:inline font-family="Helvetica">&amp;#x6D; :</fo:inline> &#x6D;
<fo:inline font-family="Helvetica">&amp;#x7B; :</fo:inline> &#x7B;
<fo:inline font-family="Helvetica">&amp;#x7C; :</fo:inline> &#x7C;
<fo:inline font-family="Helvetica">&amp;#x7D; :</fo:inline> &#x7D;
<fo:inline font-family="Helvetica">&amp;#xAC; :</fo:inline> &#xAC;
<fo:inline font-family="Helvetica">&amp;#xB0; :</fo:inline> &#xB0;
<fo:inline font-family="Helvetica">&amp;#xB1; :</fo:inline> &#xB1;
<fo:inline font-family="Helvetica">&amp;#xB5; :</fo:inline> &#xB5;
<fo:inline font-family="Helvetica">&amp;#xD7; :</fo:inline> &#xD7;
<fo:inline font-family="Helvetica">&amp;#xF7; :</fo:inline> &#xF7;
<fo:inline font-family="Helvetica">&amp;#x0192; :</fo:inline> &#x0192;
<fo:inline font-family="Helvetica">&amp;#x0391; :</fo:inline> &#x0391;
<fo:inline font-family="Helvetica">&amp;#x0392; :</fo:inline> &#x0392;
<fo:inline font-family="Helvetica">&amp;#x0393; :</fo:inline> &#x0393;
<fo:inline font-family="Helvetica">&amp;#x0395; :</fo:inline> &#x0395;
<fo:inline font-family="Helvetica">&amp;#x0396; :</fo:inline> &#x0396;
<fo:inline font-family="Helvetica">&amp;#x0397; :</fo:inline> &#x0397;
<fo:inline font-family="Helvetica">&amp;#x0398; :</fo:inline> &#x0398;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
<fo:inline font-family="Helvetica">&amp;#x0399; :</fo:inline> &#x0399;
<fo:inline font-family="Helvetica">&amp;#x039A; :</fo:inline> &#x039A;
<fo:inline font-family="Helvetica">&amp;#x039B; :</fo:inline> &#x039B;
<fo:inline font-family="Helvetica">&amp;#x039C; :</fo:inline> &#x039C;
<fo:inline font-family="Helvetica">&amp;#x039D; :</fo:inline> &#x039D;
<fo:inline font-family="Helvetica">&amp;#x039E; :</fo:inline> &#x039E;
<fo:inline font-family="Helvetica">&amp;#x039F; :</fo:inline> &#x039F;
<fo:inline font-family="Helvetica">&amp;#x03A0; :</fo:inline> &#x03A0;
<fo:inline font-family="Helvetica">&amp;#x03A1; :</fo:inline> &#x03A1;
<fo:inline font-family="Helvetica">&amp;#x03A3; :</fo:inline> &#x03A3;
<fo:inline font-family="Helvetica">&amp;#x03A4; :</fo:inline> &#x03A4;
<fo:inline font-family="Helvetica">&amp;#x03A5; :</fo:inline> &#x03A5;
<fo:inline font-family="Helvetica">&amp;#x03A6; :</fo:inline> &#x03A6;
<fo:inline font-family="Helvetica">&amp;#x03A7; :</fo:inline> &#x03A7;
<fo:inline font-family="Helvetica">&amp;#x03A8; :</fo:inline> &#x03A8;
<fo:inline font-family="Helvetica">&amp;#x03B1; :</fo:inline> &#x03B1;
<fo:inline font-family="Helvetica">&amp;#x03B2; :</fo:inline> &#x03B2;
<fo:inline font-family="Helvetica">&amp;#x03B3; :</fo:inline> &#x03B3;
<fo:inline font-family="Helvetica">&amp;#x03B4; :</fo:inline> &#x03B4;
<fo:inline font-family="Helvetica">&amp;#x03B5; :</fo:inline> &#x03B5;
<fo:inline font-family="Helvetica">&amp;#x03B6; :</fo:inline> &#x03B6;
<fo:inline font-family="Helvetica">&amp;#x03B7; :</fo:inline> &#x03B7;
<fo:inline font-family="Helvetica">&amp;#x03B8; :</fo:inline> &#x03B8;
<fo:inline font-family="Helvetica">&amp;#x03B9; :</fo:inline> &#x03B9;
<fo:inline font-family="Helvetica">&amp;#x03BA; :</fo:inline> &#x03BA;
<fo:inline font-family="Helvetica">&amp;#x03BB; :</fo:inline> &#x03BB;
<fo:inline font-family="Helvetica">&amp;#x03BD; :</fo:inline> &#x03BD;
<fo:inline font-family="Helvetica">&amp;#x03BE; :</fo:inline> &#x03BE;
<fo:inline font-family="Helvetica">&amp;#x03BF; :</fo:inline> &#x03BF;
<fo:inline font-family="Helvetica">&amp;#x03C0; :</fo:inline> &#x03C0;
<fo:inline font-family="Helvetica">&amp;#x03C1; :</fo:inline> &#x03C1;
<fo:inline font-family="Helvetica">&amp;#x03C2; :</fo:inline> &#x03C2;
<fo:inline font-family="Helvetica">&amp;#x03C3; :</fo:inline> &#x03C3;
<fo:inline font-family="Helvetica">&amp;#x03C4; :</fo:inline> &#x03C4;
<fo:inline font-family="Helvetica">&amp;#x03C5; :</fo:inline> &#x03C5;
<fo:inline font-family="Helvetica">&amp;#x03C6; :</fo:inline> &#x03C6;
<fo:inline font-family="Helvetica">&amp;#x03C7; :</fo:inline> &#x03C7;
<fo:inline font-family="Helvetica">&amp;#x03C8; :</fo:inline> &#x03C8;
<fo:inline font-family="Helvetica">&amp;#x03C9; :</fo:inline> &#x03C9;
<fo:inline font-family="Helvetica">&amp;#x03D1; :</fo:inline> &#x03D1;
<fo:inline font-family="Helvetica">&amp;#x03D2; :</fo:inline> &#x03D2;
<fo:inline font-family="Helvetica">&amp;#x03D5; :</fo:inline> &#x03D5;
<fo:inline font-family="Helvetica">&amp;#x03D6; :</fo:inline> &#x03D6;
<fo:inline font-family="Helvetica">&amp;#x2022; :</fo:inline> &#x2022;
<fo:inline font-family="Helvetica">&amp;#x2026; :</fo:inline> &#x2026;
<fo:inline font-family="Helvetica">&amp;#x2032; :</fo:inline> &#x2032;
<fo:inline font-family="Helvetica">&amp;#x2033; :</fo:inline> &#x2033;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
<fo:inline font-family="Helvetica">&amp;#x2044; :</fo:inline> &#x2044;
<fo:inline font-family="Helvetica">&amp;#x2111; :</fo:inline> &#x2111;
<fo:inline font-family="Helvetica">&amp;#x2118; :</fo:inline> &#x2118;
<fo:inline font-family="Helvetica">&amp;#x211C; :</fo:inline> &#x211C;
<fo:inline font-family="Helvetica">&amp;#x2126; :</fo:inline> &#x2126;
<fo:inline font-family="Helvetica">&amp;#x2135; :</fo:inline> &#x2135;
<fo:inline font-family="Helvetica">&amp;#x2190; :</fo:inline> &#x2190;
<fo:inline font-family="Helvetica">&amp;#x2191; :</fo:inline> &#x2191;
<fo:inline font-family="Helvetica">&amp;#x2192; :</fo:inline> &#x2192;
<fo:inline font-family="Helvetica">&amp;#x2193; :</fo:inline> &#x2193;
<fo:inline font-family="Helvetica">&amp;#x2194; :</fo:inline> &#x2194;
<fo:inline font-family="Helvetica">&amp;#x21B5; :</fo:inline> &#x21B5;
<fo:inline font-family="Helvetica">&amp;#x21D0; :</fo:inline> &#x21D0;
<fo:inline font-family="Helvetica">&amp;#x21D1; :</fo:inline> &#x21D1;
<fo:inline font-family="Helvetica">&amp;#x21D2; :</fo:inline> &#x21D2;
<fo:inline font-family="Helvetica">&amp;#x21D3; :</fo:inline> &#x21D3;
<fo:inline font-family="Helvetica">&amp;#x21D4; :</fo:inline> &#x21D4;
<fo:inline font-family="Helvetica">&amp;#x2200; :</fo:inline> &#x2200;
<fo:inline font-family="Helvetica">&amp;#x2202; :</fo:inline> &#x2202;
<fo:inline font-family="Helvetica">&amp;#x2203; :</fo:inline> &#x2203;
<fo:inline font-family="Helvetica">&amp;#x2205; :</fo:inline> &#x2205;
<fo:inline font-family="Helvetica">&amp;#x2206; :</fo:inline> &#x2206;
<fo:inline font-family="Helvetica">&amp;#x2207; :</fo:inline> &#x2207;
<fo:inline font-family="Helvetica">&amp;#x2208; :</fo:inline> &#x2208;
<fo:inline font-family="Helvetica">&amp;#x2209; :</fo:inline> &#x2209;
<fo:inline font-family="Helvetica">&amp;#x220B; :</fo:inline> &#x220B;
<fo:inline font-family="Helvetica">&amp;#x220F; :</fo:inline> &#x220F;
<fo:inline font-family="Helvetica">&amp;#x2211; :</fo:inline> &#x2211;
<fo:inline font-family="Helvetica">&amp;#x2212; :</fo:inline> &#x2212;
<fo:inline font-family="Helvetica">&amp;#x2217; :</fo:inline> &#x2217;
<fo:inline font-family="Helvetica">&amp;#x221A; :</fo:inline> &#x221A;
<fo:inline font-family="Helvetica">&amp;#x221D; :</fo:inline> &#x221D;
<fo:inline font-family="Helvetica">&amp;#x221E; :</fo:inline> &#x221E;
<fo:inline font-family="Helvetica">&amp;#x2220; :</fo:inline> &#x2220;
<fo:inline font-family="Helvetica">&amp;#x2227; :</fo:inline> &#x2227;
<fo:inline font-family="Helvetica">&amp;#x2228; :</fo:inline> &#x2228;
<fo:inline font-family="Helvetica">&amp;#x2229; :</fo:inline> &#x2229;
<fo:inline font-family="Helvetica">&amp;#x222A; :</fo:inline> &#x222A;
<fo:inline font-family="Helvetica">&amp;#x222B; :</fo:inline> &#x222B;
<fo:inline font-family="Helvetica">&amp;#x2234; :</fo:inline> &#x2234;
<fo:inline font-family="Helvetica">&amp;#x223C; :</fo:inline> &#x223C;
<fo:inline font-family="Helvetica">&amp;#x2245; :</fo:inline> &#x2245;
<fo:inline font-family="Helvetica">&amp;#x2248; :</fo:inline> &#x2248;
<fo:inline font-family="Helvetica">&amp;#x2260; :</fo:inline> &#x2260;
<fo:inline font-family="Helvetica">&amp;#x2261; :</fo:inline> &#x2261;
<fo:inline font-family="Helvetica">&amp;#x2264; :</fo:inline> &#x2264;
<fo:inline font-family="Helvetica">&amp;#x2265; :</fo:inline> &#x2265;
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
  </fo:block>
</fo:table-cell>
<fo:table-cell>
  <fo:block>
<fo:inline font-family="Helvetica">&amp;#x2282; :</fo:inline> &#x2282;
<fo:inline font-family="Helvetica">&amp;#x2283; :</fo:inline> &#x2283;
<fo:inline font-family="Helvetica">&amp;#x2284; :</fo:inline> &#x2284;
<fo:inline font-family="Helvetica">&amp;#x2286; :</fo:inline> &#x2286;
<fo:inline font-family="Helvetica">&amp;#x2287; :</fo:inline> &#x2287;
<fo:inline font-family="Helvetica">&amp;#x2295; :</fo:inline> &#x2295;
<fo:inline font-family="Helvetica">&amp;#x2297; :</fo:inline> &#x2297;
<fo:inline font-family="Helvetica">&amp;#x22A5; :</fo:inline> &#x22A5;
<fo:inline font-family="Helvetica">&amp;#x22C5; :</fo:inline> &#x22C5;
<fo:inline font-family="Helvetica">&amp;#x2320; :</fo:inline> &#x2320;
<fo:inline font-family="Helvetica">&amp;#x2321; :</fo:inline> &#x2321;
<fo:inline font-family="Helvetica">&amp;#x2329; :</fo:inline> &#x2329;
<fo:inline font-family="Helvetica">&amp;#x232A; :</fo:inline> &#x232A;
<fo:inline font-family="Helvetica">&amp;#x25CA; :</fo:inline> &#x25CA;
<fo:inline font-family="Helvetica">&amp;#x2660; :</fo:inline> &#x2660;
<fo:inline font-family="Helvetica">&amp;#x2663; :</fo:inline> &#x2663;
<fo:inline font-family="Helvetica">&amp;#x2665; :</fo:inline> &#x2665;
<fo:inline font-family="Helvetica">&amp;#x2666; :</fo:inline> &#x2666;
<fo:inline font-family="Helvetica">&amp;#xF6D9; :</fo:inline> &#xF6D9;
<fo:inline font-family="Helvetica">&amp;#xF6DA; :</fo:inline> &#xF6DA;
<fo:inline font-family="Helvetica">&amp;#xF6DB; :</fo:inline> &#xF6DB;
<fo:inline font-family="Helvetica">&amp;#xF8E5; :</fo:inline> &#xF8E5;
<fo:inline font-family="Helvetica">&amp;#xF8E6; :</fo:inline> &#xF8E6;
<fo:inline font-family="Helvetica">&amp;#xF8E7; :</fo:inline> &#xF8E7;
<fo:inline font-family="Helvetica">&amp;#xF8E8; :</fo:inline> &#xF8E8;
<fo:inline font-family="Helvetica">&amp;#xF8E9; :</fo:inline> &#xF8E9;
<fo:inline font-family="Helvetica">&amp;#xF8EA; :</fo:inline> &#xF8EA;
<fo:inline font-family="Helvetica">&amp;#xF8EB; :</fo:inline> &#xF8EB;
<fo:inline font-family="Helvetica">&amp;#xF8EC; :</fo:inline> &#xF8EC;
<fo:inline font-family="Helvetica">&amp;#xF8ED; :</fo:inline> &#xF8ED;
<fo:inline font-family="Helvetica">&amp;#xF8EE; :</fo:inline> &#xF8EE;
<fo:inline font-family="Helvetica">&amp;#xF8EF; :</fo:inline> &#xF8EF;
<fo:inline font-family="Helvetica">&amp;#xF8F0; :</fo:inline> &#xF8F0;
<fo:inline font-family="Helvetica">&amp;#xF8F1; :</fo:inline> &#xF8F1;
<fo:inline font-family="Helvetica">&amp;#xF8F2; :</fo:inline> &#xF8F2;
<fo:inline font-family="Helvetica">&amp;#xF8F3; :</fo:inline> &#xF8F3;
<fo:inline font-family="Helvetica">&amp;#xF8F4; :</fo:inline> &#xF8F4;
<fo:inline font-family="Helvetica">&amp;#xF8F5; :</fo:inline> &#xF8F5;
<fo:inline font-family="Helvetica">&amp;#xF8F6; :</fo:inline> &#xF8F6;
<fo:inline font-family="Helvetica">&amp;#xF8F7; :</fo:inline> &#xF8F7;
<fo:inline font-family="Helvetica">&amp;#xF8F8; :</fo:inline> &#xF8F8;
<fo:inline font-family="Helvetica">&amp;#xF8F9; :</fo:inline> &#xF8F9;
<fo:inline font-family="Helvetica">&amp;#xF8FA; :</fo:inline> &#xF8FA;
<fo:inline font-family="Helvetica">&amp;#xF8FB; :</fo:inline> &#xF8FB;
<fo:inline font-family="Helvetica">&amp;#xF8FC; :</fo:inline> &#xF8FC;
<fo:inline font-family="Helvetica">&amp;#xF8FD; :</fo:inline> &#xF8FD;
<fo:inline font-family="Helvetica">&amp;#xF8FE; :</fo:inline> &#xF8FE;
  </fo:block>
</fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
  </fo:block>

  <fo:block font-family="Helvetica"  font-size="12pt">
 Some special characters:
  </fo:block>
  <fo:block space-after.optimum="10pt" font-family="Helvetica">
Euro ( dec 8364, hex 20AC): &#x20AC;
  </fo:block>


  <fo:block space-after.optimum="10pt" font-family="Helvetica">

  </fo:block>

</fo:flow>
</fo:page-sequence>
</fo:root>
</#escape>
