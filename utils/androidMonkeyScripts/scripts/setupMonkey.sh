#notes for human operating this script:
##copy scripts directory to machine
##copy android-sdk_r24.4.1-linux.tgz to home directory
##copy Tracerunner jar to scripts directory

cp /home/ubuntu/scripts/.bashrc /home/ubuntu/.bashrc
source .bashrc
cd /home/ubuntu
tar -xvvzf android-sdk_r24.4.1-linux.tgz
sudo apt-get update
sudo apt-get install build-essential
sudo apt-get install openjdk-7-jre openjdk-7-jdk
export ALLTHINGS="1";for i in {2..159}; do ALLTHINGS=$ALLTHINGS,$i; done
android update sdk -u -a -t $ALLTHINGS
sudo dpkg --add-architecture i386;sudo apt-get update;sudo apt-get install libc6:i386 libncurses5:i386 libstdc++6:i386;sudo apt-get install lib32z1 lib32ncurses5

mkdir /home/ubuntu/emulator
mkdir /home/ubuntu/traces
mkdir /home/ubuntu/working

touch /home/ubuntu/toTrace.txt

sudo bash -c "echo '127.0.0.1 $HOSTNAME' >> /etc/hosts"
