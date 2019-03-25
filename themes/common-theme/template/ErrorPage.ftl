<#--
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
<!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
<html>
<head>
    <title>500 Internal error</title>
    <style>
        body{
            display: flex;
            justify-content: center;
            align-items: center;
            font-family: Helvetica,Arial,sans-serif;
            color:#6d6e71;
            position:relative;
            background: #f6e9f0;
            margin:0;
            min-height:100vh;
            background: -moz-linear-gradient(-45deg, #f6e9f0 0%, #fdebeb 100%);
            background: -webkit-linear-gradient(-45deg, #f6e9f0 0%,#fdebeb 100%);
            background: linear-gradient(135deg, #f6e9f0 0%,#fdebeb 100%);
        }
        .container{
            position:relative;
            text-align:center;
        }
        .ofbiz{
            margin:40px 0;
        }
        .content{
            width:auto;
            max-width:1000px;
            margin-top:60px;
            border-radius:5px;
            background:#FFFFFF;
            padding:50px 100px 30px 100px;
            text-align:center;
            display: flex;
            flex-direction: column;
            position:relative;
            z-index:2;
            -webkit-box-shadow: 0px 0px 18px 0px rgba(159,32,100,0.11);
            -moz-box-shadow: 0px 0px 18px 0px rgba(159,32,100,0.11);
            box-shadow: 0px 0px 18px 0px rgba(159,32,100,0.11);
        }
        .content p.error-500{
            font-size:28px;
            letter-spacing:2px;
            font-weight:normal;
            color:#d22128;
        }
        .content p.error-500 span{
            display:block;
            letter-spacing:3px;
            color:#6d6e71;
            margin:0 0 10px 8px;
        }
        .content p.error-500 strong{
            font-size:150px;
            line-height:120px;
            font-weight:normal;
        }
        .content h1{
            font-size:28px;
            color:#6d6e71;
            letter-spacing:3px;
            font-weight:normal;
            text-transform: uppercase;
            margin:0 0 5px 0;
        }
        .content p{
            font-size:15px;
            text-align:justify;
            max-width:380px;
            margin:0 auto;
            line-height:20px;
        }
        .content .img{
            max-width:380px;
            margin:20px auto;
        }
        .top-right{
            position:absolute;
            top:0;
            right:0;
            z-index:0;
        }
        .bottom-left{
            position:absolute;
            bottom:0;
            left:0;
            z-index:0;
        }
    </style>
</head>
<body>
<div class="container">
    <div class="content">
        <p class="error-500"><span>ERROR MESSAGE</span></p>
        <p>${request.getAttribute("_ERROR_MESSAGE_")?replace("\n", "<br/>")}</p>
        <div class="img">
            <svg
                    xmlns:dc="http://purl.org/dc/elements/1.1/"
                    xmlns:cc="http://creativecommons.org/ns#"
                    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                    xmlns:svg="http://www.w3.org/2000/svg"
                    xmlns="http://www.w3.org/2000/svg"
                    xmlns:xlink="http://www.w3.org/1999/xlink"
                    id="svg3134"
                    version="1.1"
                    viewBox="0 0 101.20419 53.890289"
                    height="53.890289mm"
                    width="101.20419mm">
                <defs
                        id="defs3128">
                    <linearGradient
                            gradientTransform="translate(0,100)"
                            gradientUnits="userSpaceOnUse"
                            y2="4596.5923"
                            x2="3089.7664"
                            y1="4633.2754"
                            x1="3295.5352"
                            id="linearGradient2973"
                            xlink:href="#linearGradient2971" />
                    <linearGradient
                            id="linearGradient2971">
                        <stop
                                id="stop2967"
                                offset="0"
                                style="stop-color:#d23332;stop-opacity:1" />
                        <stop
                                id="stop2969"
                                offset="1"
                                style="stop-color:#a4205e;stop-opacity:1" />
                    </linearGradient>
                    <linearGradient
                            gradientTransform="translate(0,100)"
                            gradientUnits="userSpaceOnUse"
                            y2="4503.186"
                            x2="3037.604"
                            y1="4616.728"
                            x1="3071.7537"
                            id="linearGradient2981"
                            xlink:href="#linearGradient2979" />
                    <linearGradient
                            id="linearGradient2979">
                        <stop
                                id="stop2975"
                                offset="0"
                                style="stop-color:#8f2470;stop-opacity:1" />
                        <stop
                                id="stop2977"
                                offset="1"
                                style="stop-color:#282662;stop-opacity:1" />
                    </linearGradient>
                    <linearGradient
                            gradientTransform="translate(0,100)"
                            gradientUnits="userSpaceOnUse"
                            y2="4639.0312"
                            x2="3313.1592"
                            y1="4558.6782"
                            x1="3318.1951"
                            id="linearGradient2965"
                            xlink:href="#linearGradient2963" />
                    <linearGradient
                            id="linearGradient2963">
                        <stop
                                id="stop2959"
                                offset="0"
                                style="stop-color:#e97826;stop-opacity:1" />
                        <stop
                                id="stop2961"
                                offset="1"
                                style="stop-color:#d23232;stop-opacity:1" />
                    </linearGradient>
                    <linearGradient
                            gradientTransform="translate(0,100)"
                            gradientUnits="userSpaceOnUse"
                            y2="4579.4399"
                            x2="3380.7964"
                            y1="4518.3018"
                            x1="3410.6697"
                            id="linearGradient2957"
                            xlink:href="#linearGradient2955" />
                    <linearGradient
                            id="linearGradient2955">
                        <stop
                                id="stop2951"
                                offset="0"
                                style="stop-color:#f18c24;stop-opacity:1" />
                        <stop
                                id="stop2953"
                                offset="1"
                                style="stop-color:#f69923;stop-opacity:1" />
                    </linearGradient>
                </defs>
                <metadata
                        id="metadata3131">
                    <rdf:RDF>
                        <cc:Work
                                rdf:about="">
                            <dc:format>image/svg+xml</dc:format>
                            <dc:type
                                    rdf:resource="http://purl.org/dc/dcmitype/StillImage" />
                            <dc:title></dc:title>
                        </cc:Work>
                    </rdf:RDF>
                </metadata>
                <g
                        transform="translate(-50.000687,-91.563468)"
                        id="layer1">
                    <ellipse
                            style="opacity:0.93999999;vector-effect:none;fill:#e0e0e2;fill-opacity:1;stroke:none;stroke-width:5.88446999;stroke-linecap:butt;stroke-linejoin:miter;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1;paint-order:markers stroke fill"
                            id="path2338"
                            cx="100.09739"
                            cy="144.49492"
                            rx="45.061985"
                            ry="0.95883906" />
                    <g
                            id="g3007"
                            transform="matrix(0.26458333,0,0,0.26458333,-752.54101,-1126.3783)">
                        <path
                                id="path2949"
                                d="m 3099.611,4677.0045 -24.8738,0.9885 -1.0458,47.8527 c 17.4472,29.1352 46.7055,35.8466 49.6055,36.2871 l 0.416,-0.027 c 0.8342,-0.057 44.8931,7.0033 97.6738,6.7656 1.9239,0.032 3.8829,0.02 5.8672,0.019 10.4557,-0.2921 20.9114,-0.5846 31.7559,-1.3222 l 8.7597,-0.5957 c 44.0415,-4.5557 71.6919,-13.8526 91.0043,-28.8876 l -66.3278,-34.0236 -2.8629,-10.3876 c 0,0 -2.1891,-9.2804 -1.315,-13.6891 0.045,-0.2266 -2.5622,2.646 -2.7241,2.8105 -1.0324,1.0494 -2.0704,2.0877 -3.1133,3.115 -3.3439,3.294 -9.724,13.7141 -9.5894,22.6578 -2.1794,-2.3452 -2.7698,-9.6496 -2.0263,-12.2214 -8.9877,7.4803 -8.3721,17.754 -8.3721,17.754 0,0 -3.2886,-4.6275 -0.6156,-10.2737 -5.2288,4.2463 -15.2051,12.9052 -20.2609,16.7394 -2.0003,1.3931 -4.3883,3.2319 -6.3887,4.625 -27.2409,1.5348 -76.3408,-16.6686 -110.0742,-31.5723 0,-0.017 0,-0.03 -0.01,-0.047 -11.1338,-5.286 -20.3674,-12.2405 -24.4059,-16.8766 z"
                                style="fill:url(#linearGradient2973);fill-opacity:1;stroke-width:4.18057013" />
                        <path
                                id="path2939"
                                d="m 3039.457,4603.2461 c -1.7156,-0.059 -2.998,1.5601 -2.5488,3.2168 5.5069,21.5006 14.2349,40.2926 28.5684,57.5293 -1.8764,1.2006 -4.0328,1.7834 -6.502,-0.9375 -3.888,2.8901 -10.2147,3.4406 -17.0703,3.4785 5.618,2.6754 11.9512,3.2385 18.2656,1.127 -11.3013,18.5428 -15.5045,37.5612 -6.248,53.6699 0.8272,1.4378 1.7484,2.8489 2.7832,4.2265 -2.7685,-6.5328 -2.2158,-12.4448 3.2168,-21.1464 -1.3503,16.4797 0.6898,26.243 6.8847,37.2753 1.4655,2.6134 3.1699,5.3116 5.1172,8.1739 -3.1637,-10.3608 -3.1933,-19.7935 -0.7109,-28.3535 0.7854,1.5191 1.6331,2.9281 2.4785,4.3398 l 4.2109,-46.1543 23.6387,-2.1465 c -0.8815,-1.012 -1.8085,-2.0436 -2.2285,-2.8808 -0.2789,-0.1851 -0.5445,-0.3643 -0.8301,-0.5371 8.8323,-0.4897 21.3072,-7.9531 21.2207,-7.9688 -15.7359,1.5956 -30.3497,1.3516 -41.9511,-3.8828 1.4513,-4.6892 5.878,-9.7728 11.9941,-15.1016 -10.2162,3.4402 -14.8524,7.694 -16.4453,9.4668 3.1124,-6.6149 2.3826,-15.6739 1.0097,-25.1504 -1.3467,11.5951 -3.9832,20.1778 -8.1269,25.2305 -11.9354,-15.4968 -19.398,-32.2516 -24.334,-51.5234 -0.2687,-1.114 -1.2473,-1.9121 -2.3926,-1.9512 z"
                                style="fill:url(#linearGradient2981);fill-opacity:1;stroke-width:4.18057013" />
                        <path
                                id="path2936"
                                d="m 3291.9932,4703.2276 13.663,41.8837 52.2442,-6.3711 c 14.7463,-11.4801 25.148,-26.6816 35.5156,-46.6738 1.2296,-2.3164 2.4489,-4.8154 3.5895,-7.0422 l -44.626,5.588 -18.3374,-58.9756 c -13.2982,10.3972 -24.4369,23.3098 -31.589,32.1993 -5.1853,6.574 -9.573,24.8439 -10.4599,39.3917 z"
                                style="fill:url(#linearGradient2965);fill-opacity:1;stroke-width:4.18057013" />
                        <path
                                id="path22-4"
                                d="m 3333.8105,4631.791 18.4532,59.0528 44.5488,-5.4336 c 18.6958,-36.5015 25.347,-61.0535 11.6523,-70.7852 -20.7097,-14.9333 -50.1324,1.9932 -62.248,8.6836 -4.3339,2.4988 -8.4633,5.3997 -12.4063,8.4824 z"
                                style="fill:url(#linearGradient2957);fill-opacity:1;stroke-width:4.18057013" />
                        <path
                                id="path22-4-6-0-9"
                                d="m 3364.2109,4664.6016 c -0.2907,1.1205 -3.7885,7.2979 -7.9199,16.5254 -4.1314,9.2274 -10.8379,21.2782 -21.3555,33.4414 -21.0267,24.3165 -57.1996,49.168 -119.1406,52.9843 -14.5823,0.1568 -29.7998,0.1712 -42.7884,-0.1998 l 1.2787,4.653 c 13.0252,0.3721 26.9095,0.6885 41.6171,0.5293 h 0.062 0.062 c 63.1986,-3.8754 100.8825,-29.5001 122.6758,-54.7031 10.8967,-12.6016 15.8356,-24.4605 20.1276,-34.0465 4.292,-9.586 5.9952,-17.1273 6.2025,-17.9262 z"
                                style="color:#000000;font-style:normal;font-variant:normal;font-weight:normal;font-stretch:normal;font-size:medium;line-height:normal;font-family:sans-serif;font-variant-ligatures:normal;font-variant-position:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-feature-settings:normal;text-indent:0;text-align:start;text-decoration:none;text-decoration-line:none;text-decoration-style:solid;text-decoration-color:#000000;letter-spacing:normal;word-spacing:normal;text-transform:none;writing-mode:lr-tb;direction:ltr;text-orientation:mixed;dominant-baseline:auto;baseline-shift:baseline;text-anchor:start;white-space:normal;shape-padding:0;clip-rule:nonzero;display:inline;overflow:visible;visibility:visible;opacity:1;isolation:auto;mix-blend-mode:normal;color-interpolation:sRGB;color-interpolation-filters:linearRGB;solid-color:#000000;solid-opacity:1;vector-effect:none;fill:#ffffff;fill-opacity:1;fill-rule:nonzero;stroke:none;stroke-width:4.98099995;stroke-linecap:butt;stroke-linejoin:miter;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1;color-rendering:auto;image-rendering:auto;shape-rendering:auto;text-rendering:auto;enable-background:accumulate" />
                        <path
                                id="path22-4-6-2"
                                d="m 3033.2285,4604.5879 c 6.1937,27.3012 16.9356,50.23 36.9317,71.0605 20.4864,25.5095 51.8053,52.0069 98.4281,74.601 3.9716,0.5927 3.8864,-0.1719 8.2281,-1.7435 -49.3956,-22.9249 -81.8746,-50.1342 -102.707,-76.086 l -0.072,-0.088 -0.08,-0.084 c -19.4679,-20.231 -29.8067,-42.2228 -35.818,-69 z"
                                style="color:#000000;font-style:normal;font-variant:normal;font-weight:normal;font-stretch:normal;font-size:medium;line-height:normal;font-family:sans-serif;font-variant-ligatures:normal;font-variant-position:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-feature-settings:normal;text-indent:0;text-align:start;text-decoration:none;text-decoration-line:none;text-decoration-style:solid;text-decoration-color:#000000;letter-spacing:normal;word-spacing:normal;text-transform:none;writing-mode:lr-tb;direction:ltr;text-orientation:mixed;dominant-baseline:auto;baseline-shift:baseline;text-anchor:start;white-space:normal;shape-padding:0;clip-rule:nonzero;display:inline;overflow:visible;visibility:visible;opacity:1;isolation:auto;mix-blend-mode:normal;color-interpolation:sRGB;color-interpolation-filters:linearRGB;solid-color:#000000;solid-opacity:1;vector-effect:none;fill:#fffffb;fill-opacity:1;fill-rule:nonzero;stroke:none;stroke-width:5.10127163;stroke-linecap:butt;stroke-linejoin:miter;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1;color-rendering:auto;image-rendering:auto;shape-rendering:auto;text-rendering:auto;enable-background:accumulate" />
                        <g
                                style="fill:#ffffff"
                                id="g24-3-7-5"
                                transform="matrix(4.1709335,-0.28368989,0.28368989,4.1709335,3011.7193,4498.5132)">
                            <path
                                    style="fill:none;stroke:#fffffc;stroke-width:1.06773412;stroke-opacity:1"
                                    id="path22-4-6-0"
                                    d="m 49.402494,57.232911 c -4.630827,2.873375 -14.382842,6.210532 -23.858254,7.141741 -1.934467,0.190113 -3.858435,0.283733 -5.726392,0.257987 l 0.108333,2.12e-4 v 0 0 0 0 0" />
                        </g>
                    </g>
                </g>
            </svg>
        </div>
    </div>
    <div class="ofbiz">
        <svg
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:cc="http://creativecommons.org/ns#"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:svg="http://www.w3.org/2000/svg"
                xmlns="http://www.w3.org/2000/svg"
                xmlns:sodipodi="http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd"
                xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape"
                width="107.3503"
                height="37.045204"
                viewBox="0 0 107.3503 37.045204"
                version="1.1"
                id="svg25"
                sodipodi:docname="ApacheOFBiz.svg"
                inkscape:version="0.92.3 (2405546, 2018-03-11)">
            <metadata
                    id="metadata29">
                <rdf:RDF>
                    <cc:Work
                            rdf:about="">
                        <dc:format>image/svg+xml</dc:format>
                        <dc:type
                                rdf:resource="http://purl.org/dc/dcmitype/StillImage" />
                        <dc:title>OFBiz-Logo</dc:title>
                    </cc:Work>
                </rdf:RDF>
            </metadata>
            <sodipodi:namedview
                    pagecolor="#ffffff"
                    bordercolor="#666666"
                    borderopacity="1"
                    objecttolerance="10"
                    gridtolerance="10"
                    guidetolerance="10"
                    inkscape:pageopacity="0"
                    inkscape:pageshadow="2"
                    inkscape:window-width="1920"
                    inkscape:window-height="1052"
                    id="namedview27"
                    showgrid="false"
                    fit-margin-top="0"
                    fit-margin-left="0"
                    fit-margin-right="0"
                    fit-margin-bottom="0"
                    inkscape:zoom="2.73178"
                    inkscape:cx="109.09782"
                    inkscape:cy="21.531926"
                    inkscape:window-x="0"
                    inkscape:window-y="0"
                    inkscape:window-maximized="1"
                    inkscape:current-layer="OFBiz-Logo" />
            <!-- Generator: Sketch 39.1 (31720) - http://www.bohemiancoding.com/sketch -->
            <title
                    id="title2">OFBiz-Logo</title>
            <desc
                    id="desc4">Created with Sketch.</desc>
            <defs
                    id="defs17">
                <linearGradient
                        x1="323.64899"
                        y1="790.30109"
                        x2="380.14191"
                        y2="150.35954"
                        id="linearGradient-1"
                        gradientTransform="matrix(0.0908848,0,0,0.05788842,55.647944,584.97976)"
                        gradientUnits="userSpaceOnUse">
                    <stop
                            stop-color="#282662"
                            offset="0%"
                            id="stop6" />
                    <stop
                            stop-color="#792B81"
                            offset="25%"
                            id="stop8" />
                    <stop
                            stop-color="#CB2039"
                            offset="50%"
                            id="stop10" />
                    <stop
                            stop-color="#DB4F32"
                            offset="75%"
                            id="stop12" />
                    <stop
                            stop-color="#F69A25"
                            offset="100%"
                            id="stop14" />
                </linearGradient>
            </defs>
            <g
                    id="Page-1"
                    style="fill:none;fill-rule:evenodd;stroke:none;stroke-width:1"
                    transform="translate(-59.999968,-593.68383)">
                <g
                        id="OFBiz-Logo">
                    <path
                            d="m 146.60555,611.57045 c 0,-0.73136 0.22256,-1.28407 0.66768,-1.65816 0.44512,-0.37408 1.07531,-0.56112 1.89059,-0.56112 0.68408,0 1.20416,0.13029 1.56026,0.39089 0.3561,0.2606 0.53414,0.63468 0.53414,1.12226 0,0.67251 -0.20616,1.21682 -0.61848,1.63294 -0.41232,0.41612 -1.04954,0.62417 -1.91167,0.62417 -1.41502,0 -2.12252,-0.51699 -2.12252,-1.55098 z m 0.57513,15.97944 c -0.18419,0.77949 -0.98303,1.41139 -1.78366,1.41139 h -2.78084 l 2.99785,-12.68614 c 0.1842,-0.77949 0.98303,-1.41139 1.78367,-1.41139 h 2.78083 z m -33.31174,-10.43087 c 0,2.37902 -0.46386,4.50792 -1.39158,6.38676 -0.92773,1.87884 -2.19748,3.29741 -3.80929,4.25574 -1.61181,0.95833 -3.46724,1.43749 -5.56634,1.43749 -2.39897,0 -4.270803,-0.61996 -5.615539,-1.85991 -1.344737,-1.23995 -2.017094,-2.96114 -2.017094,-5.16363 0,-2.22771 0.463855,-4.27674 1.391585,-6.14718 0.927729,-1.87043 2.20685,-3.30581 3.837398,-4.30618 1.63055,-1.00036 3.5141,-1.50054 5.65068,-1.50054 2.3896,0 4.24035,0.60526 5.55228,1.81579 1.31194,1.21052 1.9679,2.90439 1.9679,5.08166 z m -7.87159,-3.64417 c -1.13389,0 -2.17405,0.38459 -3.12052,1.15378 -0.94647,0.76918 -1.68911,1.82628 -2.22794,3.17131 -0.53884,1.34503 -0.808251,2.80352 -0.808251,4.37553 0,1.23575 0.311581,2.16885 0.934751,2.79933 0.62317,0.63048 1.50169,0.94572 2.63558,0.94572 1.13389,0 2.16703,-0.36568 3.09944,-1.09704 0.93241,-0.73136 1.661,-1.76533 2.18577,-3.10195 0.52478,-1.33663 0.78716,-2.83716 0.78716,-4.50163 0,-1.19371 -0.30455,-2.11631 -0.91366,-2.76781 -0.60912,-0.6515 -1.46655,-0.97724 -2.57233,-0.97724 z m 11.94303,14.05914 c -0.18209,0.78029 -0.97851,1.41283 -1.77966,1.41283 h -2.83725 l 4.01072,-17.02306 c 0.18375,-0.77992 0.98273,-1.41217 1.78329,-1.41217 h 9.99134 l -0.42446,1.79104 c -0.18478,0.77972 -0.98403,1.4118 -1.78498,1.4118 h -5.69026 l -1.11046,4.75382 h 6.64868 l -0.4396,1.78166 c -0.19195,0.77793 -0.99717,1.40856 -1.79842,1.40856 h -5.19782 z m 18.97726,-17.02228 c 2.14595,0 3.76711,0.34255 4.86352,1.02768 1.0964,0.68513 1.6446,1.70859 1.6446,3.07044 0,1.26096 -0.38889,2.30124 -1.16668,3.12087 -0.7778,0.81963 -1.88356,1.36394 -3.31732,1.63294 v 0.0757 c 0.9371,0.21857 1.68443,0.62418 2.242,1.21683 0.55757,0.59265 0.83636,1.35132 0.83636,2.27603 0,1.92507 -0.71687,3.40879 -2.15064,4.45119 -1.43376,1.0424 -3.41569,1.56359 -5.94586,1.56359 h -7.33745 l 4.01072,-17.02307 c 0.18375,-0.77991 0.98208,-1.41216 1.78321,-1.41216 z m -3.44074,7.28961 h 2.0944 c 1.1339,0 1.9843,-0.20385 2.55125,-0.61156 0.56694,-0.40772 0.85041,-0.99406 0.85041,-1.75904 0,-1.15168 -0.79652,-1.72751 -2.38959,-1.72751 h -2.13658 z m -1.88588,7.887 h 2.53015 c 1.09641,0 1.95618,-0.24378 2.57935,-0.73136 0.62317,-0.48757 0.93476,-1.16427 0.93476,-2.03014 0,-1.36184 -0.85744,-2.04275 -2.57233,-2.04275 h -2.31931 z m 28.62413,1.85987 c -0.18295,0.78043 -0.98037,1.41309 -1.78216,1.41309 h -9.72395 l 0.49197,-2.26973 8.08244,-8.88977 h -5.5804 l 0.37314,-1.52925 c 0.18984,-0.77805 0.99327,-1.40878 1.79439,-1.40878 h 9.09166 l -0.60443,2.52191 -7.95593,8.63758 h 6.17077 z"
                            id="OFBiz"
                            inkscape:connector-curvature="0"
                            style="fill:#e05d30;stroke-width:0.07253397" />
                    <path
                            d="m 91.744038,596.92224 c 11.061582,-3.07732 19.929752,-0.80612 20.507832,5.67841 0.16943,1.90339 -0.39233,3.9786 -1.54545,6.08864 l 5.47835,-3.5e-4 c 1.32032,-2.30425 2.12505,-4.96935 1.95316,-7.06827 -0.63202,-7.69411 -12.08283,-10.06842 -26.344795,-5.912 l 0.03869,-0.95665 c 0.03721,-0.99152 -1.489125,-1.29755 -3.413183,-0.68801 -1.921192,0.61374 -3.513574,1.91206 -3.55457,2.90204 l -0.04884,1.22501 c -14.560958,6.1213 -25.433697,16.74386 -24.787937,24.60057 0.691326,8.39337 14.25795,10.45956 30.304237,4.61185 1.12428,-0.40952 2.230874,-0.84705 3.316509,-1.3095 l -3.12e-4,-3.31256 c -1.03614,0.48039 -2.122408,0.92801 -3.231407,1.33783 -12.698934,4.70087 -23.513335,2.68713 -24.153382,-4.49709 -0.583732,-6.53954 7.468382,-15.11292 18.5009,-20.14198 l -0.132313,3.31897 c -0.03877,0.99187 1.486822,1.29874 3.410599,0.68864 1.921187,-0.61073 3.513926,-1.91164 3.5538,-2.90491 l 0.148098,-3.66064 z m 3.155101,11.29588 c 0.130962,2.09885 -3.03835,5.23839 -7.078503,7.00925 -4.042315,1.7731 -7.422062,1.50756 -7.554562,-0.59059 -0.129982,-2.09822 3.041286,-5.23769 7.082137,-7.01008 4.040289,-1.77415 7.422835,-1.50819 7.550928,0.59142 z"
                            id="Bug"
                            style="fill:url(#linearGradient-1);stroke-width:0.07253397"
                            inkscape:connector-curvature="0" />
                    <path
                            d="m 165.90055,614.86375 c 0.24354,0 0.48131,0.0625 0.71332,0.18746 0.23201,0.12498 0.41274,0.30379 0.5422,0.53644 0.12947,0.23264 0.1942,0.47522 0.1942,0.72774 0,0.24995 -0.0638,0.49029 -0.19131,0.72101 -0.12754,0.23073 -0.30635,0.40986 -0.53644,0.5374 -0.23008,0.12754 -0.47074,0.1913 -0.72197,0.1913 -0.25123,0 -0.49189,-0.0638 -0.72198,-0.1913 -0.23008,-0.12754 -0.40921,-0.30667 -0.53739,-0.5374 -0.12818,-0.23072 -0.19227,-0.47106 -0.19227,-0.72101 0,-0.25252 0.065,-0.4951 0.19515,-0.72774 0.13011,-0.23265 0.31116,-0.41146 0.54317,-0.53644 0.232,-0.12498 0.46978,-0.18746 0.71332,-0.18746 z m 0,0.24034 c -0.20381,0 -0.40217,0.0522 -0.59508,0.1567 -0.19291,0.10446 -0.34384,0.25379 -0.45279,0.44799 -0.10896,0.19419 -0.16343,0.39639 -0.16343,0.60661 0,0.20893 0.0535,0.40921 0.16054,0.60084 0.10703,0.19163 0.25668,0.34096 0.44895,0.44799 0.19228,0.10703 0.39287,0.16055 0.60181,0.16055 0.20893,0 0.40953,-0.0535 0.60181,-0.16055 0.19227,-0.10703 0.34159,-0.25636 0.44798,-0.44799 0.1064,-0.19163 0.15959,-0.39191 0.15959,-0.60084 0,-0.21022 -0.0542,-0.41242 -0.16247,-0.60661 -0.10831,-0.1942 -0.25924,-0.34353 -0.4528,-0.44799 -0.19355,-0.10447 -0.39159,-0.1567 -0.59411,-0.1567 z m -0.63642,2.01306 v -1.56123 h 0.53644 c 0.1833,0 0.31596,0.0144 0.398,0.0433 0.082,0.0288 0.14741,0.0791 0.19611,0.15093 0.0487,0.0718 0.0731,0.14805 0.0731,0.22881 0,0.11408 -0.0407,0.21341 -0.12209,0.29802 -0.0814,0.0846 -0.18939,0.13202 -0.32398,0.14227 0.0551,0.0231 0.0993,0.0506 0.13267,0.0827 0.0628,0.0615 0.13971,0.16471 0.23072,0.30956 l 0.19035,0.3057 h -0.30764 l -0.13843,-0.2461 c -0.10895,-0.19355 -0.19676,-0.31468 -0.26341,-0.36339 -0.0461,-0.0359 -0.11344,-0.0538 -0.20188,-0.0538 h -0.14805 v 0.66333 z m 0.25188,-0.87867 h 0.30571 c 0.14612,0 0.24578,-0.0218 0.29898,-0.0654 0.0532,-0.0436 0.0798,-0.10126 0.0798,-0.17304 0,-0.0462 -0.0128,-0.0875 -0.0385,-0.12402 -0.0256,-0.0365 -0.0612,-0.0638 -0.10671,-0.0817 -0.0455,-0.018 -0.12978,-0.0269 -0.25283,-0.0269 H 165.516 v 0.47106 z"
                            id="®"
                            inkscape:connector-curvature="0"
                            style="fill:#e05d30;stroke-width:0.07253397" />
                    <path
                            d="m 120.0092,606.76652 -0.84524,1.91236 h -0.9192 l 3.39681,-7.57546 h 0.89806 l 3.38624,7.57546 h -0.94033 l -0.84524,-1.91236 z m 2.05499,-4.67522 -1.6852,3.84055 h 3.39681 z m 10.07683,-0.32225 c 0.51595,0.44375 0.77392,1.07063 0.77392,1.88065 0,0.8382 -0.25797,1.4871 -0.77392,1.94669 -0.51595,0.4596 -1.24232,0.6894 -2.17913,0.6894 h -1.98103 v 2.39309 h -0.86637 v -7.57546 h 2.8474 c 0.93681,0 1.66318,0.22187 2.17913,0.66563 z m -4.16016,3.68735 h 1.94933 c 0.6938,0 1.22559,-0.15143 1.59539,-0.45431 0.36979,-0.30288 0.55469,-0.74663 0.55469,-1.33126 0,-0.57053 -0.18578,-1.00195 -0.55733,-1.29427 -0.37156,-0.29231 -0.90247,-0.43846 -1.59275,-0.43846 h -1.94933 z m 6.60871,1.31012 -0.84524,1.91236 h -0.9192 l 3.39681,-7.57546 H 137.12 l 3.38624,7.57546 h -0.94033 l -0.84524,-1.91236 z m 2.05499,-4.67522 -1.6852,3.84055 h 3.3968 z m 9.59346,-0.71582 c 0.48249,0.19547 0.90863,0.46576 1.27842,0.81091 l -0.52299,0.66034 c -0.29584,-0.29584 -0.64009,-0.52828 -1.03278,-0.69733 -0.39268,-0.16904 -0.79505,-0.25357 -1.20711,-0.25357 -0.56349,0 -1.0812,0.13295 -1.55312,0.39885 -0.47193,0.2659 -0.84436,0.62688 -1.1173,1.08296 -0.27295,0.45608 -0.40941,0.95706 -0.40941,1.50294 0,0.54237 0.13646,1.04246 0.40941,1.5003 0.27294,0.45784 0.64625,0.81971 1.11994,1.08561 0.47369,0.2659 0.99051,0.39884 1.55048,0.39884 0.41206,0 0.81267,-0.0792 1.20183,-0.23772 0.38916,-0.15848 0.73518,-0.38212 1.03806,-0.67091 l 0.53355,0.59695 c -0.38388,0.35923 -0.82058,0.64361 -1.31012,0.85317 -0.48953,0.20954 -0.99139,0.31432 -1.50558,0.31432 -0.71493,0 -1.37086,-0.17081 -1.96782,-0.51243 -0.59695,-0.34162 -1.07063,-0.8065 -1.42106,-1.39464 -0.35042,-0.58815 -0.52563,-1.23264 -0.52563,-1.93349 0,-0.6938 0.17697,-1.33037 0.53092,-1.90971 0.35394,-0.57934 0.83202,-1.03894 1.43426,-1.37879 0.60224,-0.33986 1.26257,-0.50979 1.98103,-0.50979 0.51419,0 1.01252,0.0977 1.49502,0.29319 z m 8.27277,7.3034 v -3.33342 h -4.56429 v 3.33342 h -0.86637 v -7.57546 h 0.86637 v 3.42322 h 4.56429 v -3.42322 h 0.86637 v 7.57546 z m 4.02545,-6.74078 v 2.48817 h 3.81942 v 0.83467 h -3.81942 v 2.59383 h 4.41637 v 0.82411 h -5.28274 v -7.57546 h 5.14011 v 0.83468 z"
                            id="APACHE"
                            inkscape:connector-curvature="0"
                            style="fill:#282662;stroke-width:0.07253397" />
                </g>
            </g>
        </svg>
    </div>
</div>
<div class="top-right">
    <svg
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:cc="http://creativecommons.org/ns#"
            xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:svg="http://www.w3.org/2000/svg"
            xmlns="http://www.w3.org/2000/svg"
            xmlns:xlink="http://www.w3.org/1999/xlink"
            id="svg4630"
            version="1.1"
            viewBox="0 0 103.89402 172.09494"
            height="172.09494mm"
            width="103.89402mm">
        <defs
                id="defs4624">
            <linearGradient
                    gradientTransform="matrix(-0.26458333,0,0,-0.26458333,839.39308,1318.2247)"
                    gradientUnits="userSpaceOnUse"
                    y2="4933.9526"
                    x2="2718.0952"
                    y1="4404.6724"
                    x1="2390.8035"
                    id="linearGradient2470"
                    xlink:href="#linearGradient2963" />
            <linearGradient
                    id="linearGradient2963">
                <stop
                        id="stop2959"
                        offset="0"
                        style="stop-color:#e97826;stop-opacity:1" />
                <stop
                        id="stop2961"
                        offset="1"
                        style="stop-color:#d23232;stop-opacity:1" />
            </linearGradient>
        </defs>
        <metadata
                id="metadata4627">
            <rdf:RDF>
                <cc:Work
                        rdf:about="">
                    <dc:format>image/svg+xml</dc:format>
                    <dc:type
                            rdf:resource="http://purl.org/dc/dcmitype/StillImage" />
                    <dc:title></dc:title>
                </cc:Work>
            </rdf:RDF>
        </metadata>
        <g
                transform="translate(-106.49747,0.6493372)"
                id="layer1">
            <path
                    style="opacity:0.08900003;fill:url(#linearGradient2470);fill-opacity:1;stroke:none;stroke-width:0.26458332px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1"
                    d="m 106.49938,-0.6493372 103.65044,0.06699 c 0,0 0.24167,137.8763572 0.24167,172.0279572 C 194.14658,86.301976 106.02462,100.09249 106.49938,-0.6493372 Z"
                    id="path2462" />
        </g>
    </svg>
</div>
<div class="bottom-left">
    <svg
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:cc="http://creativecommons.org/ns#"
            xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:svg="http://www.w3.org/2000/svg"
            xmlns="http://www.w3.org/2000/svg"
            xmlns:xlink="http://www.w3.org/1999/xlink"
            id="svg4033"
            version="1.1"
            viewBox="0 0 91.812927 146.7365"
            height="146.7365mm"
            width="91.812927mm">
        <defs
                id="defs4027">
            <linearGradient
                    gradientTransform="matrix(-0.26458333,0,0,-0.26458333,916.99951,1314.7743)"
                    gradientUnits="userSpaceOnUse"
                    y2="4298.6235"
                    x2="3464.0454"
                    y1="3842.3198"
                    x1="3123.0879"
                    id="linearGradient2460"
                    xlink:href="#linearGradient2979" />
            <linearGradient
                    id="linearGradient2979">
                <stop
                        id="stop2975"
                        offset="0"
                        style="stop-color:#8f2470;stop-opacity:1" />
                <stop
                        id="stop2977"
                        offset="1"
                        style="stop-color:#282662;stop-opacity:1" />
            </linearGradient>
        </defs>
        <metadata
                id="metadata4030">
            <rdf:RDF>
                <cc:Work
                        rdf:about="">
                    <dc:format>image/svg+xml</dc:format>
                    <dc:type
                            rdf:resource="http://purl.org/dc/dcmitype/StillImage" />
                    <dc:title></dc:title>
                </cc:Work>
            </rdf:RDF>
        </metadata>
        <g      transform="translate(0.7040734,-151.73208)"
                id="layer1">
            <path   style="opacity:0.125;fill:url(#linearGradient2460);fill-opacity:1;stroke:none;stroke-width:0.26458332px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1"
                    d="M -0.7040734,298.46858 V 151.73208 C 31.56287,239.33445 96.998351,202.92977 90.682481,298.16045 Z"
                    id="path2452" />
        </g>
    </svg>
</div>
</body></html>