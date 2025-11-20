#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

CONC=1
ITERS=1

# Initial setup.
#mkdir $(DIR)/tmpapp
#cp $ARGO_HOME/benchmarks/data/apps/hy-js-thumbnail.zip $(DIR)/tmpapp
#cd $(DIR)/tmpapp
#unzip hy-js-thumbnail.zip
#cd -

#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-js-dynamic-html.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-js-hello-world.so"
export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/tmpapp/hy-js-thumbnail.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-js-uploader.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-jv-file-hashing.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-jv-hello-world.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-jv-httprequest.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-py-bfs.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-py-compression.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-py-dna.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-py-dynamic-html.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-py-hello-world.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-py-mst.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-py-pagerank.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-py-thumbnail.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-py-uploader.so"

# Disabled (sub-processes).
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-jv-classify.zip"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-py-video-processing.so"
#export BENCHMARKS="$BENCHMARKS $ARGO_HOME/benchmarks/data/apps/hy-jv-video-processing.so"

mkdir $(DIR)/results &> /dev/null
for benchmark in $BENCHMARKS
do
    bdir=$(DIR)/results/$(basename $benchmark)
    mkdir $bdir &> /dev/null
    rm -f snapshot.mem
    rm -f snapshot.meta
    $(DIR)/run-env.sh /bin/time -v $(DIR)/main normal     $benchmark $CONC $ITERS 2>&1 | tee $bdir/normal.log
    $(DIR)/run-env.sh /bin/time -v $(DIR)/main checkpoint $benchmark $CONC $ITERS 2>&1 | tee $bdir/checkpoint.log
    $(DIR)/run-env.sh /bin/time -v $(DIR)/main restore    $benchmark $CONC $ITERS 2>&1 | tee $bdir/restore.log
done
