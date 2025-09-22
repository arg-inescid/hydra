#!/bin/bash

if [[ -z "${ARGO_HOME}" ]]; then
    echo "ARGO_HOME is not defined. Exiting..."
    exit 1
fi

DATA_IP="172.18.0.1"
DATA_PORT=8000
DATA_ADDRESS="http://$DATA_IP:$DATA_PORT"

BENCHMARK_HOME_DIR="$ARGO_HOME/../faastion/benchmarks/src/java/SeBS"

FAASTION_BENCHMARKS=(fa_cl  fa_dn  fa_dh  fa_bf  fa_co  fa_ms  fa_pr  fa_th  fa_up  fa_vp) # full list
FAASTION_BENCHMARKS=(fa_dn  fa_dh  fa_bf  fa_co  fa_ms  fa_pr  fa_up  fa_vp) # working
FAASTION_BENCHMARKS=(fa_cl)

declare -A BENCHMARK_CODE
BENCHMARK_CODE[fa_cl]="$BENCHMARK_HOME_DIR/gv-classify/build/libclassify.so"
BENCHMARK_CODE[fa_dn]="$BENCHMARK_HOME_DIR/gv-dna-visualization/build/libdna.so"
BENCHMARK_CODE[fa_dh]="$BENCHMARK_HOME_DIR/gv-dynamic-html/build/libdynamic-html.so"
BENCHMARK_CODE[fa_bf]="$BENCHMARK_HOME_DIR/gv-native-bfs/build/libbfs.so"
BENCHMARK_CODE[fa_co]="$BENCHMARK_HOME_DIR/gv-native-compression/build/libzip.so"
BENCHMARK_CODE[fa_ms]="$BENCHMARK_HOME_DIR/gv-native-mst/build/libmst.so"
BENCHMARK_CODE[fa_pr]="$BENCHMARK_HOME_DIR/gv-native-pagerank/build/libpagerank.so"
BENCHMARK_CODE[fa_th]="$BENCHMARK_HOME_DIR/gv-thumbnail/build/libthumbnail.so"
BENCHMARK_CODE[fa_up]="$BENCHMARK_HOME_DIR/gv-uploader/build/libuploader.so"
BENCHMARK_CODE[fa_vp]="$BENCHMARK_HOME_DIR/gv-video-processing/build/libvideoprocessing.so"


declare -A BENCHMARK_CODE_VANILLA
BENCHMARK_CODE_VANILLA[fa_cl]="$BENCHMARK_HOME_DIR/gv-classify/build/libclassify_vanilla.so"
BENCHMARK_CODE_VANILLA[fa_dn]="$BENCHMARK_HOME_DIR/gv-dna-visualization/build/libdna_vanilla.so"
BENCHMARK_CODE_VANILLA[fa_dh]="$BENCHMARK_HOME_DIR/gv-dynamic-html/build/libdynamic-html_vanilla.so"
BENCHMARK_CODE_VANILLA[fa_bf]="$BENCHMARK_HOME_DIR/gv-native-bfs/build/libbfs_vanilla.so"
BENCHMARK_CODE_VANILLA[fa_co]="$BENCHMARK_HOME_DIR/gv-native-compression/build/libzip_vanilla.so"
BENCHMARK_CODE_VANILLA[fa_ms]="$BENCHMARK_HOME_DIR/gv-native-mst/build/libmst_vanilla.so"
BENCHMARK_CODE_VANILLA[fa_pr]="$BENCHMARK_HOME_DIR/gv-native-pagerank/build/libpagerank_vanilla.so"
BENCHMARK_CODE_VANILLA[fa_th]="$BENCHMARK_HOME_DIR/gv-thumbnail/build/libthumbnail_vanilla.so"
BENCHMARK_CODE_VANILLA[fa_up]="$BENCHMARK_HOME_DIR/gv-uploader/build/libuploader_vanilla.so"
BENCHMARK_CODE_VANILLA[fa_vp]="$BENCHMARK_HOME_DIR/gv-video-processing/build/libvideoprocessing_vanilla.so"


declare -A BENCHMARK_ENTRYPOINTS
BENCHMARK_ENTRYPOINTS[fa_cl]="com.classify.Classify"
BENCHMARK_ENTRYPOINTS[fa_dn]="com.dna.DNAVisualization"
BENCHMARK_ENTRYPOINTS[fa_dh]="com.dynamic_html.DynamicHTML"
BENCHMARK_ENTRYPOINTS[fa_bf]="com.jni.BFS"
BENCHMARK_ENTRYPOINTS[fa_co]="com.jni.ZIPCompression"
BENCHMARK_ENTRYPOINTS[fa_ms]="com.jni.MST"
BENCHMARK_ENTRYPOINTS[fa_pr]="com.jni.PageRank"
BENCHMARK_ENTRYPOINTS[fa_th]="com.thumbnail.Thumbnail"
BENCHMARK_ENTRYPOINTS[fa_up]="com.uploader.Uploader"
BENCHMARK_ENTRYPOINTS[fa_vp]="com.videoprocessing.VideoProcessing"


declare -A BENCHMARK_PAYLOADS
BENCHMARK_PAYLOADS[fa_cl]="{}"
BENCHMARK_PAYLOADS[fa_dn]="{}"
BENCHMARK_PAYLOADS[fa_dh]="{}"
BENCHMARK_PAYLOADS[fa_bf]="{}"
BENCHMARK_PAYLOADS[fa_co]="{}"
BENCHMARK_PAYLOADS[fa_ms]="{}"
BENCHMARK_PAYLOADS[fa_pr]="{}"
BENCHMARK_PAYLOADS[fa_th]="{}"
BENCHMARK_PAYLOADS[fa_up]="{}"
BENCHMARK_PAYLOADS[fa_vp]="{}"
