#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

CONC=10
ITERS=1
BENCHMARKS="$BENCHMARKS $(DIR)/test7-svm-truffle-function-py/libcompression.so"
BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/src/javascript/gv-dynamic-html/build/libdynamichtml.so"

mkdir $(DIR)/results &> /dev/null
for benchmark in $BENCHMARKS
do
    bdir=$(DIR)/results/$(basename $benchmark)
    mkdir $bdir &> /dev/null
    rm -f snapshot.mem
    rm -f snapshot.meta
    setarch -R /bin/time -v $(DIR)/main normal     $benchmark $CONC $ITERS 2>&1 | tee $bdir/normal.log
    setarch -R /bin/time -v $(DIR)/main checkpoint $benchmark $CONC $ITERS 2>&1 | tee $bdir/checkpoint.log
    setarch -R /bin/time -v $(DIR)/main restore    $benchmark $CONC $ITERS 2>&1 | tee $bdir/restore.log
done