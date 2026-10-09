#build for s905x franklin default Android 9

### define OPTARG
# -d DIM Changed;  1=Change(defaut); 0=No change 
# -c Had change Config file; 0=no Config file switch(default); 1= Had Config file Switch
# -s add su;   1=had su(default); 0=no su 
# -m set inner.version in vendor/starnet/build.prop
### end define

### build with java 1.8
#. /opt/jdk_switch.sh jdk8

### configure ####
#
MAKE_PATCH=0
UPDATE_PATCH=0
#-d Change DIM for OTA
MAKE_OTA_DIM=1
# -c Make New Project
CONFIG_FILE=0
# -v set OEM Version
CONFIG_FLAG=0
# -m set inner.version to vendor/starnet/build.prop, default qaz
INNER_VERSION=INNER_1.0.1

#PATCH_NAME=patch_s9
### end configure ####

LUNCH_NUM=p291_iptv-userdebug
TASK_NUM=8 #VNC ALLOW ONE TASK ONLY
REPO_TASK_NUM=8 #VNC ALLOW ONE TASK ONLY
AML_BOARD=p291_iptv
export STARNET_ENABLE=true

#CUSTOM=def

if [ $# -gt 1 ]; then
while getopts c:d:s:v:km OPT
do
	case $OPT in
	c) CONFIG_FILE=$OPTARG
	;;
	d) MAKE_OTA_DIM=$OPTARG
	;;
	s) FLAG_SU=$OPTARG
	;;
	v) 
		CONFIG_VERSION=$OPTARG
		CONFIG_FLAG=1;
	;;
	k) FLAG_XIRI_2110=1 ;;
	m) INNER_VERSION=$OPTARG ;;
	*) echo "err Input.."
	;;	
	esac
done
fi

# for oem target  at /vender/...
OEM_TARGET=p291_tb611
ZIP=update_sys_s905l3ap_tb611.zip

export PATH=/opt/gcc-linaro-6.3.1-2017.02-x86_64_arm-linux-gnueabihf/bin:$PATH
export OEM_TARGET
export FLAG_SU
export FLAG_WHX_CFG
export FLAG_DEBUG

if [ "$FLAG_XIRI_2110" = "1" ]; then
    export BUILD_XIRI_PRODUCT="2110"
    echo "BUILD_XIRI_PRODUCT set to 2110"
fi

if [ $CONFIG_FILE -ne 0 ];then    
    echo "start to config ..."
    . vendor/$OEM_TARGET/configs/clean.sh
    . vendor/$OEM_TARGET/configs/config.sh 
    if [ $? -ne 0 ];then
      echo "add config failed,exit!"
      return 
    fi
    echo "config finished"
fi

### MAKE inner.version (vendor/starnet/build.prop, loaded by init on device)
FILE="vendor/starnet/build.prop"
echo "MAKE INNER VERSION: "$INNER_VERSION
awk -F = -v nvar=$INNER_VERSION -v OFS="=" -v vStr="inner.version" \
	'{if($1==vStr) ($1=vStr) ($2=nvar);print }' $FILE > $FILE.awk
mv $FILE.awk $FILE
rm -f out/target/product/$AML_BOARD/vendor/starnet/build.prop

### MAKE VERSION  
#BUILD_OEM_VERSION_STR="ro.product.releaseversion"
#if [ $CONFIG_FLAG -ne 0 ]; then
#	echo "MAKE VERSION DATE FLAG ..." 
#	BUILD_OEM_VERSION=$CONFIG_VERSION
#	echo "MAKE VERSION DATE FLAG ..."$CONFIG_VERSION   
#	awk -F = -v nvar=$BUILD_OEM_VERSION -v OFS="=" -v vStr=$BUILD_OEM_VERSION_STR \
#		'{if($1==vStr) ($1=vStr) ($2=nvar);print }' device/amlogic/$AML_BOARD/$AML_BOARD.mk  > device/amlogic/$AML_BOARD/$AML_BOARD.mk.awk
#	mv device/amlogic/$AML_BOARD/$AML_BOARD.mk.awk device/amlogic/$AML_BOARD/$AML_BOARD.mk
#	
#	rm -rf out/target/product/$AW_BOARD/system/build.prop
#fi
BUILD_OEM_VERSION="1.0.1"
FILE="device/amlogic/"$AML_BOARD/$AML_BOARD.mk
# 读取包含 ro.product.build.dim 的行
line=$(grep "ro.product.build.dim=" "$FILE" | head -1)
# 提取=号后面的值: P291_TB611_V1.0.4_20260408
value="${line#*=}"
# 按 _ 分隔成4部分存入数组
IFS='_' read -r -a parts <<< "$value"
# 第1部分: 项目名 parts[0] = P291
# 第2部分: 产品名 parts[1] = TB611
# 第3部分: 版本 parts[2] = V1.0.4
# 第4部分: 日期 parts[3] = 20260408
# 提取第3部分并去掉V前缀: 1.0.4
BUILD_OEM_VERSION="${parts[2]#V}"
if [ $CONFIG_FLAG -ne 0 ]; then
	BUILD_OEM_VERSION=$CONFIG_VERSION	
fi
echo "BUILD OEM VERSION: "$BUILD_OEM_VERSION
export BUILD_OEM_VERSION
### MAKE DIM  
OTA_DIM_FIX="P291_TB612_V"$BUILD_OEM_VERSION
BUILD_OTA_DIM=$OTA_DIM_FIX"_"$(date +%Y%m%d)
BUILD_OTA_DIM_STR="ro.product.build.dim"
if [ $MAKE_OTA_DIM -ne 0 ] ; then
	echo "MAKE DIM DATE FLAG ..." 
	awk -F = -v nvar=$BUILD_OTA_DIM -v OFS="=" -v vStr=$BUILD_OTA_DIM_STR \
		'{if($1==vStr) ($1=vStr) ($2=nvar);print }' device/amlogic/$AML_BOARD/$AML_BOARD.mk  > device/amlogic/$AML_BOARD/$AML_BOARD.mk.awk
	mv device/amlogic/$AML_BOARD/$AML_BOARD.mk.awk device/amlogic/$AML_BOARD/$AML_BOARD.mk
	
	rm -rf out/target/product/$AW_BOARD/system/build.prop
fi

###############################################
# make otapackage
###############################################
echo "make otapackage Start..."

. build/envsetup.sh

export PROJECT_ID=t249
export PROJECT_TYPE=sdmobile
lunch $LUNCH_NUM

make installclean -j8
#make dtbimage -j$TASK_NUM
make otapackage -j$TASK_NUM
if [ $? -ne 0 ];then
   echo "make otapackage -j$TASK_NUM failed,exit!"
   return
fi

mv out/target/product/$AML_BOARD/aml_upgrade_package.img out/target/product/$AML_BOARD/$OEM_TARGET.img
mv out/target/product/$AML_BOARD/$AML_BOARD-ota-eng.${USER}.zip out/target/product/$AML_BOARD/$ZIP
echo "make otapackage finished"
date "+%Y-%m-%d %H:%M:%S"

###############################################


####################################################
#echo "last step Make factory_update_param.aml"
#    cd out/target/product/$AML_BOARD/
#    rm factory_update_param.aml
#    echo --update_package=/sdcard/$ZIP > factory_update_param.aml

if [ $UPDATE_PATCH -ne 0 ];then    
    echo --update_patch=/sdcard/$PATCH_NAME.zip >> factory_update_param.aml
fi    
    #echo "--copy_custom_files=/sdcard/mediafile" >> factory_update_param.aml
    echo "--wipe_data" >> factory_update_param.aml
    echo "--wipe_cache" >> factory_update_param.aml
    echo "--wipe_media" >> factory_update_param.aml
    cd -
    echo "All jobs done!"
#    return
######################################################    
