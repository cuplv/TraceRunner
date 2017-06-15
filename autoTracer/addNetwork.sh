DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TEMPFILE=$(dirname $1)/tmp
python $DIR/../utils/AppTransformations/addNetwork.py --apk $1 --out $TEMPFILE
mv ${TEMPFILE} $1
