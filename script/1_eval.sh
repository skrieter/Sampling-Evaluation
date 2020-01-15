#! /bin/bash
CONFIG_DIR=$1
SCRIPT_DIR=${PWD}/subscripts

cd ${SCRIPT_DIR}
sh clean_jar.sh

cd ${SCRIPT_DIR}
sh build_eval_jar.sh

cd ${SCRIPT_DIR}
sh run_eval.sh ${CONFIG_DIR}