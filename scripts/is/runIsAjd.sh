#!/usr/bin/bash
for k in {1..100}
do
  echo no noise run number ${k}
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ajd -is 1 -quiet -data is1 &
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ajd -is 3 -quiet -data is3 &
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ajd -is 10 -quiet -data is10 & 
  java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ajd -is 15 -quiet -data is15 
done
for i in 0.025 0.05 0.075 0.10 0.125 0.15 0.175 0.20
do
  for k in {1..100}
  do
    echo ea=${i} ep=${i} run number ${k}
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -ajd -is 1 -quiet -data is1 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -ajd -is 3 -quiet -data is3 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -ajd -is 10 -quiet -data is10 &
    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar Simulation -g 100000 -q 0.8 -ea ${i} -ep ${i} -ajd -is 15 -quiet -data is15 
  done
done





























