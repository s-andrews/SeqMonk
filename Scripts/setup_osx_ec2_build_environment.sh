## This script only works if you have access to a graphical environment
## on OSX so before running the commands below you need to set this up
##
## Create and launch an EC2 OSX instance
## Log in conventionally
## ssh -i BioTrain.pem ec2-user@XXX.XXX.XXX.XXX
##
## Run:
##
## sudo launchctl enable system/com.apple.screensharing
## sudo launchctl load -w /System/Library/LaunchDaemons/com.apple.screensharing.plist
##
## Set a password:
## passwd ec2-user
##
## Log out then log in again with
## ssh -L 5900:localhost:5900 -i BioTrain.pem ec2-user@XXX.XXX.XXX.XXX
##
## Now you can run the commands below before running the "build_seqmonk_release.sh" script *AS ROOT*



mkdir git
cd git
git clone https://github.com/s-andrews/SeqMonk.git
brew install wget
brew install gnu-tar
brew install create-dmg
brew install ant
brew install openjdk@21
cd
wget --no-check-certificate "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jre_x64_mac_hotspot_21.0.5_11.tar.gz"
tar -xzf OpenJDK21U-jre_x64_mac_hotspot_21.0.5_11.tar.gz
mv jdk-21.0.5+11-jre jre-osx
wget --no-check-certificate "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jre_x64_windows_hotspot_21.0.5_11.zip"
unzip  OpenJDK21U-jre_x64_windows_hotspot_21.0.5_11.zip
mv jdk-21.0.5+11-jre jre-windows64
wget --no-check-certificate "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jre_x64_linux_hotspot_21.0.5_11.tar.gz"
tar -xzf OpenJDK21U-jre_x64_linux_hotspot_21.0.5_11.tar.gz
mv jdk-21.0.5+11-jre jre-linux64
export PATH=/usr/local/opt/openjdk@21/bin:$PATH
cd git/SeqMonk
ant
cd
wget --no-check-certificate "https://www.bioinformatics.babraham.ac.uk/projects/seqmonk/seqmonk_v1.48.1_osx64.dmg"
hdiutil attach seqmonk_v1.48.1_osx64.dmg
cp -R /Volumes/SeqMonk/SeqMonk.app .
umount /Volumes/SeqMonk
ln -s git/SeqMonk/scripts/build_seqmonk_release.sh .
