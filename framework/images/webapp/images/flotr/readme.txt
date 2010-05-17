Author: Bas Wenneker
Website: <http://www.solutoire.com>
Flotr Project Page: <http://www.solutoire.com/flotr/> 
Contact: <b.wenneker@gmail.com>
Date: Jan 12, 2009

=============================
License
=============================
Flotr is released under the MIT License (see license.txt).

=============================
Flotr 0.2.0-alpha
=============================
Flotr is a javascript plotting library based on the Prototype Javascript Framework 
(version 1.6.0.2 at the moment) and inspired by Flot (written by Ole Laursen). 
Flotr enables you to draw appealing graphs in most modern browsers with an
easy-to-learn syntax. It comes with great features like legend support, negative
value support, mouse tracking, selection support, zoom support, event hooks, CSS
styling support and much more.

=============================
Documentation and Examples
=============================
Find documentation and examples at <http://www.solutoire.com/flotr/docs/>. The example
files are also included in the Flotr zip package (see the examples directory).

=============================
Build Flotr from svn
=============================
To build your own version from svn, do the following. Get the svn trunk url from 

<http://code.google.com/p/flotr/source/checkout>

Then check out the trunk into a local folder. You can change the flotr js file, 
which resides in flotr/prototype. Then, download the yui-compressor (2.3.5) from 

<http://www.julienlecomte.net/yuicompressor/>

Place the contents of the zip in the root of your svn working-copy. With ANT, run
build.compress. After building, a new folder 'release' has been created. Here you can 
find the compressed flotr js.

=============================
UnitTesting Flotr with jsUnit
=============================
To test Flotr using the jsUnit UnitTests. Download jsUnit from 

<http://www.jsunit.net>

Place the contents of the zip (a folder named 'jsunit') into the trunk svn folder. Change
the firefox_exe and ie7_exe properties in the ANT build.xml. You might also have to 
change the prototype_testsuite property to point at the tests/testRunner.html file. When everything's
configured, run 'test.prototype' with ANT. The browsers will open up and start running the unit test.
I use Eclipse with Aptana installed as plugin. ANT is already integrated into Eclipse.
If you are looking for a way to set up Eclipse and ANT, check out the following article:

<http://solutoire.com/2007/05/31/automate-aptana-with-ant/>