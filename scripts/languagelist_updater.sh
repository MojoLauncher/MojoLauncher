#!/bin/bash

THISDIR=`dirname $0`
LANGFILE=$THISDIR/../app_mojolauncher/src/main/assets/language_list.txt

rm -f $LANGFILE
echo $THISDIR/../app_mojolauncher/src/main/res/values-* | xargs -- basename -a > $LANGFILE

