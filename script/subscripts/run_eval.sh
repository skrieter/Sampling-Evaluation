#!/bin/sh
cd ../..
java -da -Xmx2g -jar pc_sampling_evaluation.jar de.ovgu.featureide.sampling.SimpleTWiseEvaluator $1
