#! /bin/bash

# Read name of system and kconfig root file
PARAM_SYSTEM_NAME=$1
PARAM_CONFIG_FILE=$2
echo ${PARAM_SYSTEM_NAME}
echo ${PARAM_CONFIG_FILE}

# Go to main directory
SCRIPT_DIR=$PWD
cd ../..
PATH_CURRENT=$PWD

# Set paths to kclause tool, system, and output directory
PATH_EXEC=${PATH_CURRENT}/script/subscripts/Kclause/
PATH_SYSTEM=${PATH_CURRENT}/resources/systems/${PARAM_SYSTEM_NAME}/
PATH_OUT=${PATH_CURRENT}/gen/kclause/${PARAM_SYSTEM_NAME}/
PATH_MODELS=${PATH_CURRENT}/resources/models/${PARAM_SYSTEM_NAME}/

# Create output directory
mkdir -p ${PATH_OUT}

# Copy kclause executables
cp -fr ${PATH_EXEC}check_dep ${PATH_SYSTEM}check_dep
cp -fr ${PATH_EXEC}dimacs.py ${PATH_SYSTEM}dimacs.py

cd ${SCRIPT_DIR}
PREPARE_FILE=systems/$1/prepare.sh
if [ -f "$PREPARE_FILE" ]; then
echo ${PREPARE_FILE} 
source ${PREPARE_FILE}
fi

# Go to main directory
cd ${PATH_SYSTEM}

# Execute kclause and write results to output dir
./check_dep --dimacs ${PARAM_CONFIG_FILE} | tee ${PATH_OUT}kconfig.kmax | python2 dimacs.py -d --include-nonvisible-bool-defaults --remove-orphaned-nonvisibles > ${PATH_OUT}kconfig.dimacs

# Clean resulting dimacs file
cd ${SCRIPT_DIR}
python clean_dimacs.py ${PATH_OUT} kconfig.dimacs

CLEAN_FILE=../systems/$1/clean.py
if [ -f "$CLEAN_FILE" ]; then
echo ${CLEAN_FILE} 
python ${CLEAN_FILE} ${PATH_OUT} _kconfig.dimacs
fi

# Move result to model directory
mkdir -p ${PATH_MODELS}
cp ${PATH_OUT}_kconfig.dimacs ${PATH_MODELS}model.dimacs
