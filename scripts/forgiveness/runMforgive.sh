#!/usr/bin/bash
for k in {1..10}
do
  echo no noise run number ${k}
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -fa -quiet -m 300 &
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -fr -quiet -m 300 &
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -fa -quiet -m 500 &
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -fr -quiet -m 500 &
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -fa -quiet -m 1000 &
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -fr -quiet -m 1000 &
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -fa -quiet -m 2500 &
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -fr -quiet -m 2500 &
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -fa -quiet -m 5000 &
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -fr -quiet -m 5000
done
for i in 0.025 0.05 0.075 0.10 0.125 0.15 0.175 0.20
do
  for k in {1..10}
  do
    echo ea=${i} ep=${i} run number ${k}
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -fa -quiet -m 300 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -fr -quiet -m 300 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -fa -quiet -m 500 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -fr -quiet -m 500 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -fa -quiet -m 1000 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -fr -quiet -m 1000 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -fa -quiet -m 2500 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -fr -quiet -m 2500 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -fa -quiet -m 5000 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -fr -quiet -m 5000
  done
done
