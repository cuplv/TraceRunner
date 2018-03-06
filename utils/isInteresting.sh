APP_ROOT=$1
GETSTRINGINST="0"
DISMISSINST="0"
SHOWINST="0"
EXECUTEINST="0"
STARTACTIVITYINST="0"
GETRESOURCESINST="0"
RECYCLEINST="0"

for f in `find $APP_ROOT -name "*.java"`
do
	GETSTRINGINST=$(expr $GETSTRINGINST + `cat $f |grep getString |egrep "\(.*\)" |wc -l`)
	DISMISSINST=$(expr $DISMISSINST + `cat $f |grep dismiss |egrep "\(\ *\)" |wc -l`)
	SHOWINST=$(expr $SHOWINST + `cat $f |grep show |egrep "\(.*\)" |wc -l`)
	EXECUTEINST=$(expr $EXECUTEINST + `cat $f |grep execute |egrep "\(.*\)" |wc -l`)
	STARTACTIVITYINST=$(expr $STARTACTIVITYINST + `cat $f |grep startActivity |egrep "\(.*\)" |wc -l`)
	GETRESOURCESINST=$(expr $GETRESOURCESINST + `cat $f |grep getResources |egrep "\(.*\)" |wc -l`)
	RECYCLEINST=$(expr $RECYCLEINST + `cat $f |grep recycle |egrep "\(.*\)" |wc -l`)
done
echo "getString: $GETSTRINGINST"
echo "dimiss: $DISMISSINST"
echo "show: $SHOWINST"
echo "execute: $EXECUTEINST"
echo "startActivity: $STARTACTIVITYINST"
echo "getResources: $GETRESOURCESINST"
echo "recycle: $RECYCLEINST"
