#!/usr/bin/perl -w
#Licensed to the Apache Software Foundation (ASF) under one
#or more contributor license agreements.  See the NOTICE file
#distributed with this work for additional information
#regarding copyright ownership.  The ASF licenses this file
#to you under the Apache License, Version 2.0 (the
#"License"); you may not use this file except in compliance
#with the License.  You may obtain a copy of the License at
#
#http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing,
#software distributed under the License is distributed on an
#"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#KIND, either express or implied.  See the License for the
#specific language governing permissions and limitations
#under the License.

use strict;
use warnings;

use Data::Dumper;
use File::Copy;

my %dirs;

my %bases = (
	'ofbiz'			=> 'debian/ofbiz',
	'specialpurpose'	=> 'debian/ofbiz-specialpurpose',
);

my $appDirsRe = qw/(applications|framework|specialpurpose)/;
my @ignore = qw(
OPTIONAL_LIBRARIES
NOTICE
ant
LICENSE
hot-deploy/README.txt
.project
applications/accounting/lib/README
applications/content/lib/uno/README
applications/content/index/indexhere.txt
ant.bat
stopofbiz.sh
startofbiz.bat
framework/example/lib/lib.txt
framework/example/webapp/webapp.txt
framework/logs/README
framework/images/webapp/images/catalog/dirholder.txt
framework/appserver/README
framework/shark/lib/README
framework/data/README
README
.classpath
ij.ofbiz
KEYS
APACHE2_HEADER
rc.ofbiz
);
#startofbiz.sh

my %ignore = map({$_ => 1} @ignore);

my @unknown;
my %scripts;

sub basename($) {
	my $target = $_[0];
	$target =~ s,/[^/]+$,,;
	return $target;
}

sub _mkdir($) {
	return mkdir($_[0]) || !system('mkdir', '-p', $_[0]);
}

sub copylink($$$) {
	my ($base, $destdir, $file) = @_;
	my $target = "$base$destdir/$file";
	#print("Symlinking ($file) ($target)\n");
	if (-f $file) {
		_mkdir(basename($target)) || die("a");
		link($file, $target) || die("b");
	} else {
		_mkdir($target) || die("c");
	}
	my $symlink = "$base/usr/share/ofbiz/$file";
	_mkdir(basename($symlink));
	symlink("$destdir/$file", $symlink) || die("f: $symlink: $!");
}
system('rm', '-rf', 'debian/ofbiz', 'debian/ofbiz-specialpurpose');

open(FIND, '-|', qw(find -not -path */.git/* -not -name .gitignore -printf %P\0)) || die("Couldn't run find");
$/ = "\0";
while (<FIND>) {
	chomp;
	#print("{$_}\n");
	next if (m,^debian/,);
	next if (exists($ignore{$_}));
#	next if (m,^(LICENSE|NOTICE|OPTIONAL_LIBRARIES|ant(\.bat)?|\.(project|classpath)|(stop|start)ofbiz\.sh|startofbiz\.bat|(ij|rc)\.ofbiz)$,);
	next if (m,(^|.*/)build\.xml$,);
	#print("1\n");
	next if (m,^$appDirsRe/[^/]+/(build/classes|src|testdef)/.*,);
	next if (m,^runtime/(catalina/work|data/derby|logs)/.*,);
	next if (m,^\.hg(|/.*),);
	#print("2\n");
	my $type = undef;
	if ($_ eq 'framework/entity/config/entityengine.xml') {
		$type = 'ucf';
	} elsif (-f m,(^|.*/)[^/]+\.css$,) {
		$type = 'conffile';
	} elsif (m,^$appDirsRe/[^/]+/webapp/.*/WEB-INF/(controller|web|regions)\.xml$,) {
		$type = 'conffile';
	} elsif ($_ eq 'specialpurpose/assetmaint/webapp/assetmaint/WEB-INF/facility-controller.xml') {
		$type = 'conffile';
	} elsif ($_ eq 'runtime/data/derby.properties') {
		$type = 'conffile';
	} elsif (m,^$appDirsRe/[^/]+/email/[^/]+/[^/]+\.ftl$,) {
		$type = 'conffile';
	} elsif (m,^$appDirsRe/[^/]+/data/[^/]+\.xml$,) {
		$type = 'conffile';
	} elsif (m,^$appDirsRe/[^/]+/ofbiz-component\.xml$,) {
		$type = 'conffile';
	} elsif (m,^$appDirsRe/component-load\.xml$,) {
		$type = 'conffile';
	} elsif (-f && m,^$appDirsRe/[^/]+/servicedef/services.*\.xml$,) {
		$type = 'conffile';
	} elsif (-f && m,^$appDirsRe/[^/]+/(dtd|entitydef|script|servicedef|widget)/.*$,) {
		$type = 'code';
	} elsif (m,^$appDirsRe/[^/]+/webapp/.*/[^/]+(\.(bsh|ftl|jsp|gif|htc|ico|jar|jpg|js|png)|(Forms?|Menus)\.xml)$,) {
		$type = 'code';
	} elsif (m,^$appDirsRe/[^/]+/webapp/.*/WEB-INF/[^/]+\.tld$,) {
		$type = 'code';
	} elsif (-f && m,^$appDirsRe/[^/]+/(config|templates|fieldtype)/.*$,) {
		$type = 'conffile';
	} elsif (m,^$appDirsRe/[^/]+/(build/lib/[^/]+\.jar|lib/.*\.jar)$,) {
		$type = 'code';
	} elsif (m,^framework/common/webcommon/.*\.ftl$,) {
		$type = 'code';
	} elsif (-f && m,^specialpurpose/pos/screens/.*$,) {
		$type = 'code';
	} elsif ($_ eq 'startofbiz.sh') {
		$type = 'conffile';
	} elsif ($_ eq 'applications/content/template/survey/genericsurvey.ftl') {
		$type = 'code';
	} elsif ($_ eq 'ofbiz.jar') {
		$type = 'code';
	} elsif (-f && m,^runtime/(logs|catalina|data)/README$,) {
		next;
	} elsif ($_ eq 'runtime/catalina/catalina-users.xml') {
		$type = 'varlib';
	} elsif (m,^runtime/catalina/[^/]+$,) {
		$type = 'varcache';
	} elsif (-d && $_ eq 'runtime/logs') {
		$type = 'varlog';
	} elsif (m,^runtime/logs/.*$,) {
		next;
	} elsif (-d && m,^runtime/tmp$,) {
		$type = 'varlib';
	} elsif (m,^runtime/(data|output)/.*$,) {
		$type = 'varlib';
	} elsif (-f) {
		$type = 'code';
	} elsif ($_ eq 'rc.ofbiz.for.debian') {
		next;
	} else {
		next;
	}
	my $pkg;
	if (m,^specialpurpose/.*,) {
		$pkg = 'ofbiz-specialpurpose';
	} elsif (m,^applications/.*,) {
		$pkg = 'ofbiz-applications';
	} else {
		$pkg = 'ofbiz-framework';
	}
	my $base = 'debian/' . $pkg;
	my $file = $_;
	print(STDERR "$type: $file\n") if ($file =~ m/^.*runtime.*/);
	if ($type eq 'code') {
		my $target = "$base/usr/share/ofbiz/$file";
		#print("Copying ($file) ($target)\n");
		_mkdir(basename($target)) || die("1");
		link($file, $target) || die("2");
	} elsif ($type eq 'conffile') { # && $file =~ m,^.*/(ofbiz-component|component-load|data/.*)\.xml$,) {
		copylink($base, '/etc/ofbiz', $file);
	} elsif ($type eq 'ucf') {
		copylink($base, '/etc/ofbiz', $file);
		_mkdir(basename("$base/usr/share/ofbiz/ucf/$file"));
		rename("$base/etc/ofbiz/$file", "$base/usr/share/ofbiz/ucf/$file");
		my $postinst = <<_EOF_;
trap 'rm -f "\$tmpconffile"' EXIT
tmpconffile=`tempfile -m 644`
munge_conffile "\$tmpconffile" "$file"
ucf --debconf-ok "\$tmpconffile" /etc/ofbiz/$file
ucfr ofbiz /etc/ofbiz/$file
rm -f "\$tmpconffile"
trap '' EXIT
_EOF_
		push(@{$scripts{$pkg}->{'postinst'}->{'configure'}}, $postinst);
		my $postrm = <<_EOF_;
for ext in '~' '%' .bak .dpkg-tmp .dpkg-new .dpkg-old .dpkg-dist;  do rm -f /etc/ofbiz/$file\$ext; done
rm -f /etc/ofbiz/$file
if which ucf >/dev/null; then ucf --debconf-ok --purge /etc/ofbiz/$file; fi
if which ucfr >/dev/null; then ucfr --purge ofbiz /etc/ofbiz/$file; fi
_EOF_
		push(@{$scripts{$pkg}->{'postrm'}->{'purge'}}, $postrm);
	} elsif ($type =~ m/^var(cache|lib|log|tmp)$/) {
		my $new = "/var/$1/ofbiz";
		copylink($base, $new, $file);
		my $postrm = <<_EOF_;
if dpkg-statoverride --list "$new/$file" > /dev/null; then dpkg-statoverride --remove "$new/$file"; fi
rm -rf "$new/$file"
_EOF_
		push(@{$scripts{$pkg}->{'postrm'}->{'purge'}}, $postrm);
		my $postinst = <<_EOF_;
if ! dpkg-statoverride --list "$new/$file" > /dev/null; then
	dpkg-statoverride --add ofbiz ofbiz 2775 "$new/$file"
	chown ofbiz:ofbiz "$new/$file"
	chmod 2755 "$new/$file"
fi
_EOF_
		push(@{$scripts{$pkg}->{'postinst'}->{'configure'}}, $postinst);
	} else {
		die("Unknown type($type) on file($file)");
	}
}


close(FIND);
my $postinst = '';
push(@{$scripts{'ofbiz-framework'}->{'postinst'}->{'configure'}}, $postinst);
foreach my $pkg (keys(%scripts)) {
	foreach my $script (keys(%{$scripts{$pkg}})) {
		open(SCRIPT, ">> debian/$pkg.$script.debhelper");
		print(SCRIPT "case \"\$1\" in\n");
		my $segments = $scripts{$pkg}->{$script};
		foreach my $arg (keys(%$segments)) {
			my $label = $arg;
			if ($label eq 'configure') {
				print(SCRIPT "\t(reconfigure|configure)\n");
			} elsif ($arg =~ m/[\s|]/) {
				print(SCRIPT "\t(\"$arg\")\n");
			} else {
				print(SCRIPT "\t($arg)\n");
			}
			print(SCRIPT join('', map("\t\t$_\n", split(/\n/, join("\n", @{$segments->{$arg}})))));
			print(SCRIPT "\t;;\n");
		}
		print(SCRIPT "esac\n");
		close(SCRIPT);
	}
}
print(join('', map("$_\n", @unknown)));
