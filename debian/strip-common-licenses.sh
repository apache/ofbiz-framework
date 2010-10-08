#!/bin/sh

mode=start
skipping=
find_license=
while IFS="" read line; do
	case "$mode" in
		(start)
			case "$line" in
				(=========================================================================)
					mode=in_header
					;;
				(*)
					echo "Bad start line: $line"
					exit 1
					;;
			esac
			;;
		(in_header)
			case "$line" in
				(=========================================================================)
					mode=license_lead
					;;
				(*)
					;;
			esac
			;;
		(license_lead)
			case "$line" in
				(=========================================================================)
					mode=license_text
					;;
				(*)
					;;
			esac
			;;
		(license_text)
			case "$line" in
				("                                 Apache License")
					skipping=1
					echo "On debian systems, the full Apache 2.0 license is located at: "
					echo "/usr/share/common-licenses/Apache-2.0"
					;;
				(=========================================================================)
					skipping=
					mode=license_lead
					;;
			esac
			;;
	esac
	if [ "z" = "z$skipping" ]; then
		echo "$line"
	fi
done < LICENSE
