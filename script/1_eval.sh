#! /bin/bash
CONFIG_DIR=$1

JAR=pc_sampling_evaluation.jar
PREFIX=de.ovgu.featureide.sampling.

# Go to main directory
cd ..

# Clean eval jar
ant clean

# Build eval jar
ant

# Pause
read -p "Press [Enter] key to continue..."

# Set new run
java -da -Xmx2g -jar ${JAR} de.ovgu.featureide.fm.benchmark.OutputCleaner $1

# Run eval jar
java -da -Xmx4g -jar ${JAR} ${PREFIX}PCExtractor ${CONFIG_DIR} 1_extract
java -da -Xmx4g -jar ${JAR} ${PREFIX}PCConverter ${CONFIG_DIR} 2_convert
java -da -Xmx4g -jar ${JAR} ${PREFIX}PCGrouper ${CONFIG_DIR} 3_group
java -da -Xmx4g -jar ${JAR} ${PREFIX}TWiseSampler ${CONFIG_DIR} 4_sample
java -da -Xmx14g -jar ${JAR} ${PREFIX}TWiseEvaluator ${CONFIG_DIR} 5_eval
