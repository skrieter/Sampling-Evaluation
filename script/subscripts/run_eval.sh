#!/bin/sh
cd ../..

JAR=pc_sampling_evaluation.jar
PREFIX=de.ovgu.featureide.sampling.

read -p "Press [Enter] key to continue..."

java -da -Xmx2g -jar ${JAR} de.ovgu.featureide.fm.benchmark.OutputCleaner $1
#read -p "Press [Enter] key to continue..."
java -da -Xmx4g -jar ${JAR} ${PREFIX}PCExtractor $1 1_extract
#read -p "Press [Enter] key to continue..."
java -da -Xmx4g -jar ${JAR} ${PREFIX}PCConverter $1 2_convert
#read -p "Press [Enter] key to continue..."
java -da -Xmx4g -jar ${JAR} ${PREFIX}PCGrouper $1 3_group
#read -p "Press [Enter] key to continue..."
java -da -Xmx4g -jar ${JAR} ${PREFIX}TWiseSampler $1 4_sample
#read -p "Press [Enter] key to continue..."
java -da -Xmx4g -jar ${JAR} ${PREFIX}TWiseEvaluator $1 5_eval
