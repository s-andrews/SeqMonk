#!/bin/bash

# This script is only really designed to work on the Babraham
# build system.  It takes the current version of seqmonk and
# builds the release files for it ready for upload to the web 
# site.
#
# For the OSX release we need a couple of additional packages
# from brew, specifically 
#
# brew install gnu-tar
# brew install create-dmg

VERSION=$1

if test -z $VERSION
then
	echo "VERSION must be passed"
	exit 1
fi

cd ~/Data

# Do windows first
echo "Copying windows version"
cp -r ~/git/SeqMonk/bin ./SeqMonk

echo "Copying windows jre"
cp -r jre-windows64 SeqMonk/jre

echo "Removing unwanted bits"
# We get a warning from trying to delete . and .. but that's OK
rm -rf SeqMonk/.* SeqMonk/Windows SeqMonk/build.xml

echo "Compressing into zip file"
zip -rq seqmonk_v${VERSION}_windows64.zip SeqMonk

echo "Cleaning up"
rm -rf SeqMonk



# Do linux next
echo "Copying linux version"
cp -r ~/git/SeqMonk/bin ./SeqMonk

echo "Copying linux jre"
cp -r jre-linux64 SeqMonk/jre

echo "Removing unwanted bits"
# We get a warning from trying to delete . and .. but that's OK
rm -rf SeqMonk/.* SeqMonk/Windows SeqMonk/build.xml

echo "Compressing into tar file"
# We installed gnu-tar (gtar) from brew to get a version 
# which doesn't give us warning when we uncompress on linux
gtar -czf seqmonk_v${VERSION}_linux64.tar.gz SeqMonk

echo "Cleaning up"
rm -rf SeqMonk


# Finally OSX
echo "Cleaning OSX app"
rm -rf SeqMonk.app/Contents/MacOS/*

echo "Copying OSX version"
cp -r ~/git/SeqMonk/bin/* ./SeqMonk.app/Contents/MacOS/

echo "Copying osx jre"
cp -r jre-osx ./SeqMonk.app/Contents/MacOS/jre

echo "Removing unwanted bits"
# We get a warning from trying to delete . and .. but that's OK
rm -rf ./SeqMonk.app/Contents/MacOS/.* ./SeqMonk.app/Contents/MacOS/Windows ./SeqMonk.app/Contents/MacOS/build.xml

echo "Fixing Plist file"
# Put the new version in the Plist file
sed "s/%%VERSION%%/${VERSION}/" ./SeqMonk.app/Contents/Info.plist.template > ./SeqMonk.app/Contents/Info.plist


# Put the app into a directory called SeqMonk
echo "Copying app"
mkdir SeqMonk
cp -r SeqMonk.app SeqMonk/

# Make the pretty dmg
echo "Compressing into dmg file"
create-dmg \
--volname "SeqMonk" \
--volicon "/Users/andrewss/git/SeqMonk/uk/ac/babraham/SeqMonk/Resources/seqmonk.icns" \
--background "/Users/andrewss/git/SeqMonk/uk/ac/babraham/SeqMonk/Resources/monk_dmg_background.png" \
--window-pos 200 120 \
--window-size 800 400 \
--icon-size 100 \
--icon "SeqMonk.app" 200 160 \
--app-drop-link 600 155 \
seqmonk_v${VERSION}_osx64.dmg \
SeqMonk

# Clean up the folder
rm -rf SeqMonk



